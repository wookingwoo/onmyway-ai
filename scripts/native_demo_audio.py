"""Capture only an authenticated, local Android emulator's speaker output.

No microphone injection, host loopback capture, cloud speech or app credentials.
The emulator's ephemeral local RPC token is read in memory and never logged.
"""
from array import array
import os
from pathlib import Path
import threading
import time
import wave


def aligned_pcm(packets, started, seconds, rate=24000):
    """Keep output gaps and source speed; timestamps use host epoch seconds."""
    output = bytearray(round(seconds * rate) * 2)
    for timestamp, data in packets:
        if len(data) % 2:
            raise ValueError("Incomplete signed-16 PCM sample")
        offset = round((timestamp - started) * rate)
        source = max(0, -offset)
        destination = max(0, offset)
        count = min(len(data) // 2 - source, len(output) // 2 - destination)
        if count > 0:
            output[destination * 2:(destination + count) * 2] = data[source * 2:(source + count) * 2]
    return bytes(output)


class EmulatorAudio:
    def __init__(self, sdk, serial):
        import sys
        sys.path[:0] = [str(sdk / "python"), str(sdk)]
        import grpc
        import emulator_controller_pb2 as pb
        import emulator_controller_pb2_grpc as rpc
        configurations = [dict(line.split("=", 1) for line in p.read_text().splitlines() if "=" in line)
                          for p in (Path(os.environ["LOCALAPPDATA"]) / "Temp/avd/running").glob("*.ini")]
        config = next(c for c in configurations if c.get("avd.id") == "OnMyWay_Pleos" and c.get("port.serial") == serial.removeprefix("emulator-"))
        self.channel = grpc.insecure_channel("127.0.0.1:" + config["grpc.port"])
        self.packets, self.error = [], None

        def record():
            try:
                stub = rpc.EmulatorControllerStub(self.channel)
                stream = stub.streamAudio(pb.AudioFormat(samplingRate=24000, channels=pb.AudioFormat.Mono, format=pb.AudioFormat.AUD_FMT_S16),
                                          metadata=[("authorization", "Bearer " + config["grpc.token"])], timeout=180)
                for packet in stream:
                    timestamp = packet.timestamp / 1_000_000
                    if abs(timestamp - time.time()) > 3:
                        raise ValueError("Emulator audio clock differs from capture clock")
                    self.packets.append((timestamp, bytes(packet.audio)))
            except grpc.RpcError as error:
                if error.code() not in [grpc.StatusCode.CANCELLED]:
                    self.error = error.code().name
            except Exception as error:
                self.error = type(error).__name__
        self.thread = threading.Thread(target=record, daemon=True)
        self.thread.start()

    def finish(self, target, started, seconds):
        self.channel.close()
        self.thread.join(5)
        if self.error or self.thread.is_alive():
            raise RuntimeError("Emulator output capture failed: " + str(self.error))
        data = aligned_pcm(self.packets, started, seconds)
        target.parent.mkdir(exist_ok=True)
        with wave.open(str(target), "wb") as output:
            output.setparams((1, 2, 24000, 0, "NONE", "not compressed"))
            output.writeframes(data)
        samples = array("h", data)
        return {"packets": len(self.packets), "seconds": len(samples) / 24000,
                "peak": max((abs(s) for s in samples), default=0), "clock": "emulator epoch microseconds, aligned to screenrecord host start"}
