"""Record the isolated native film package on an official Pleos AVD.

Authored UI scenario, actual screen recording and actual native restriction gate.
Does not touch the live/demo app, send provider requests, or clear app data.
"""
import argparse
from hashlib import sha256
import json
from pathlib import Path
import re
import subprocess
import threading
import time
import urllib.request
import xml.etree.ElementTree as ET


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--adb',required=True)
    p.add_argument('--serial',default='emulator-5556')
    args=p.parse_args()
    root=Path('artifacts/final-films/pleos');root.mkdir(parents=True,exist_ok=True)
    cmd=[args.adb,'-s',args.serial]
    def adb(*a,binary=False):
        r=subprocess.run(cmd+list(a),check=True,capture_output=True)
        return r.stdout if binary else r.stdout.decode('utf-8').strip()
    assert args.serial.startswith('emulator-') and adb('emu','avd','name').startswith('OnMyWay_Pleos')
    active=subprocess.run(cmd+['shell','pidof','screenrecord'],capture_output=True)
    assert active.returncode==1 and not active.stdout.strip(), 'Another recording is active or recorder status is unknown'
    def request(path,method='GET'):
        with urllib.request.urlopen(urllib.request.Request('http://127.0.0.1:8088'+path,method=method),timeout=12) as r:
            return json.load(r)
    session='/browser/session?client=onmyway-native-film'
    stop=threading.Event()
    def heartbeat():
        while not stop.wait(.5):
            try:request(session,'POST')
            except OSError:stop.set()
    def tree():
        adb('shell','uiautomator','dump','/sdcard/omw-native-film.xml')
        return ET.fromstring(adb('shell','cat','/sdcard/omw-native-film.xml'))
    def click(label):
        node=next(n for n in tree().iter('node') if n.get('text')==label and n.get('enabled')=='true')
        x,y,r,b=map(int,re.findall(r'\d+',node.get('bounds')))
        adb('shell','input','tap',str((x+r)//2),str((y+b)//2))
    def snapshot(name):
        xml=tree();(root/(name+'.xml')).write_text(ET.tostring(xml,encoding='unicode'),encoding='utf-8')
        (root/(name+'.png')).write_bytes(adb('exec-out','screencap','-p',binary=True))
        return [n.get('text') for n in xml.iter('node') if n.get('text')]
    def vehicle(gear,speed):
        request('/vehicle/controls?speed='+str(speed)+'&accelerator=0&brake=0&steering=0','PUT')
        request('/vehicle/gear?gear='+gear,'PUT')
        time.sleep(2.5)
        state=request('/vehicle/state')
        assert state['gear']['actual']['value']==gear and state['controls']['speed_kph']==speed
        return {'gear':gear,'speed_kph':speed}
    records=[]
    stages=[('request',24,None),('clarify',14,'요청 보내기'),('baseline',23,'수령 완료까지'),
            ('change',16,'말로 조건 바꾸기'),('revised',17,'수정한 계획 보기'),
            ('guidance-laundry',23,'확인하고 출발'),('arrival-laundry',10,'목적지 도착'),
            ('next-medicine',19,'수령을 마쳤어요'),('guidance-medicine',8,'확인하고 출발'),
            ('arrival-medicine',10,'목적지 도착'),('home',19,'수령을 마쳤어요'),
            ('guidance-home',9,'집으로 출발'),('finished',23,'목적지 도착')]
    request(session,'POST');thread=threading.Thread(target=heartbeat,daemon=True);thread.start()
    try:
        vehicle('Park',0)
        # Only terminate/restart this separate filming package; no data clearing.
        adb('shell','am','force-stop','ai.onmyway.car.film')
        adb('shell','am','start','-n','ai.onmyway.car.film/ai.onmyway.car.FilmActivity')
        time.sleep(2)
        for i,(name,seconds,action) in enumerate(stages):
            remote='/sdcard/omw-native-'+name+'.mp4'
            proc=subprocess.Popen(cmd+['shell',f'screenrecord --size 1920x1080 --bit-rate 16000000 --time-limit 90 {remote} & p=$!; echo $p; wait $p'],
                 stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,encoding='utf-8',creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
            pid=proc.stdout.readline().strip();assert pid.isdigit()
            start=time.monotonic();beats=[]
            try:
                time.sleep(.7)
                if action:click(action)
                labels=snapshot(name)
                expected=f'{i+1} / 13'
                assert any(expected in label for label in labels), (name,labels)
                beats.append({'name':'state-visible','at':round(time.monotonic()-start,3)})
                if name=='guidance-laundry':
                    time.sleep(5)
                    vehicle('Drive',30)
                    locked_start=round(time.monotonic()-start,3)
                    labels=snapshot('driving-lock')
                    assert '정차 후 이용해 주세요' in labels
                    beats.append({'name':'actual-drive-lock','at':round(time.monotonic()-start,3)})
                    time.sleep(8)
                    locked_end=round(time.monotonic()-start,3)
                    vehicle('Park',0)
                    snapshot('park-restored')
                    beats.append({'name':'park-restored','at':round(time.monotonic()-start,3)})
                remaining=seconds-(time.monotonic()-start)
                if remaining>0:time.sleep(remaining)
            finally:
                early=proc.poll() is not None
                if not early:adb('shell','kill','-2',pid)
                proc.communicate(timeout=20)
                assert not early,'Early screenrecord termination'
            path=root/(name+'.mp4');adb('pull',remote,str(path))
            record={'id':name,'file':path.name,'sha256':sha256(path.read_bytes()).hexdigest(),'beats':beats,
                    'elapsed':round(time.monotonic()-start,3),'action':action}
            if name=='guidance-laundry':record['locked_window']=[locked_start,locked_end]
            records.append(record)
            (root/'native-capture-manifest.json').write_text(json.dumps({'scope':'Actual official Pleos screen recording of isolated native filming prototype; authored scenario, staged arrival, real restrictions',
                'complete':len(records)==len(stages),'avd':'OnMyWay_Pleos','package':'ai.onmyway.car.film','captures':records},ensure_ascii=False,indent=2),encoding='utf-8')
            print('CAPTURED '+name,flush=True)
    finally:
        vehicle('Park',0);stop.set();thread.join(timeout=2);request(session+'&closing=1','POST')


if __name__=='__main__':main()
