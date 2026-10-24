"""Local screen recording for an explicitly selected isolated AVD demo."""
import atexit
from pathlib import Path
import subprocess
import time


class DemoRecording:
    def __init__(self, adb, serial, output):
        self.command = [adb, '-s', serial]
        self.output = Path(output)
        self.process = None
        self.pid = None
        self.name = None
        atexit.register(self.stop)

    def run(self, *args, check=True):
        return subprocess.run(self.command + list(args), check=check, capture_output=True,
                              text=True, encoding='utf-8').stdout.strip()

    def start(self, name):
        assert self.process is None
        assert self.run('emu', 'avd', 'name').startswith('OnMyWay_Pleos')
        assert not self.run('shell', 'pidof', 'screenrecord', check=False), 'Another recording is active'
        assert name in {'normal', 'tight', 'driving'}
        self.name = name
        self.remote = '/sdcard/omw-intent-' + name + '.mp4'
        self.process = subprocess.Popen(self.command + ['shell',
            'screenrecord --size 1920x1080 --bit-rate 6000000 --time-limit 180 ' +
            self.remote + ' & recorder=$!; echo $recorder; wait $recorder'],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, encoding='utf-8',
            creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
        self.pid = self.process.stdout.readline().strip()
        assert self.pid.isdigit(), 'Recording PID was not established'
        time.sleep(.5)

    def stop(self):
        if self.process is None:
            return
        process, self.process = self.process, None
        ended_early = process.poll() is not None
        if self.pid and self.pid.isdigit() and not ended_early:
            self.run('shell', 'kill', '-2', self.pid, check=False)
        process.communicate(timeout=15)
        self.output.mkdir(parents=True, exist_ok=True)
        self.run('pull', self.remote, str(self.output / (self.name + '-raw.mp4')))
        self.pid = None
        assert not ended_early, 'Recording ended before the scenario; do not use truncated footage'
