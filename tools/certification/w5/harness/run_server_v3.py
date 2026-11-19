from pathlib import Path
import os,subprocess,time,json,hashlib,datetime,threading,queue,sys
A=Path(sys.argv[1]).resolve();D=A/'runtime';R=A/'receipts';T=A/'tmp';R.mkdir(exist_ok=True);T.mkdir(exist_ok=True)
args=json.loads(Path(sys.argv[2]).read_text(encoding='utf8')) if len(sys.argv)>2 else {'product':False,'args':[],'autoStopSeconds':2700}
java=Path(r'C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot\bin\java.exe');cmd=[str(java),'-Xms128M','-Xmx2G',*args.get('args',[]),'-jar','purpur-26.2-2620.jar','--nogui']
env={k:os.environ[k] for k in ('PATH','SystemRoot','WINDIR','COMSPEC','PATHEXT','USERPROFILE','APPDATA','LOCALAPPDATA') if k in os.environ};env.update({'TEMP':str(T),'TMP':str(T)})
flags=getattr(subprocess,'CREATE_NO_WINDOW',0);p=subprocess.Popen(cmd,cwd=D,env=env,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,creationflags=flags,text=True,encoding='utf8',errors='replace',bufsize=1)
control=R/'control.txt';control.write_text('',encoding='utf8');journal_path=R/'command-journal.jsonl';journal_path.open('x',encoding='utf8').close();out=(R/'output.log').open('x',encoding='utf8');journal=journal_path.open('a',encoding='utf8')
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
launch={'pid':p.pid,'command':cmd,'productAgent':args.get('product',False),'productReadyClaim':False,'onlineMode':True,'port':52704,'serverJarSHA256':sha(D/'purpur-26.2-2620.jar'),'serverJarBytes':(D/'purpur-26.2-2620.jar').stat().st_size,'observerSHA256':sha(D/'plugins'/'w4-gameplay-evidence-probe.jar') if (D/'plugins'/'w4-gameplay-evidence-probe.jar').exists() else None,'pluginSHA256':sha(D/'plugins'/'achievetodo.jar') if (D/'plugins'/'achievetodo.jar').exists() else None,'world':'stage2b-w4-attempt01','secretValuesLogged':False}
(R/'launch.json').write_text(json.dumps(launch,indent=2)+'\n',encoding='utf8')
lines=queue.Queue();
def pump():
 try:
  for line in p.stdout:lines.put(line)
 finally:lines.put(None)
reader=threading.Thread(target=pump,daemon=True);reader.start();start=time.monotonic();stop=False;automatic=False;eof=False
while not eof or p.poll() is None:
 try:
  line=lines.get(timeout=.12)
  if line is None:eof=True
  else:out.write(line);out.flush()
 except queue.Empty:pass
 try:
  try:
   text=control.read_text(encoding='utf8')
  except OSError:
   text=''
  if text.strip():
   try:
    control.write_text('',encoding='utf8')
   except OSError:
    continue
   for command in [x.strip() for x in text.splitlines() if x.strip()]:
    journal.write(json.dumps({'utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'command':command},ensure_ascii=False)+'\n');journal.flush()
    try:p.stdin.write(command+'\n');p.stdin.flush()
    except (BrokenPipeError,OSError):pass
    if command.lower()=='stop':stop=True
 except OSError:pass
 if time.monotonic()-start>args.get('autoStopSeconds',2700) and not stop:
  automatic=True;stop=True
  journal.write(json.dumps({'utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'command':'stop','automatic':True})+'\n');journal.flush()
  try:p.stdin.write('stop\n');p.stdin.flush()
  except (BrokenPipeError,OSError):pass
if p.poll() is None:p.wait()
reader.join(timeout=5);out.flush();out.close();journal.close()
(R/'execution.json').write_text(json.dumps({'exitCode':p.returncode,'elapsedSeconds':round(time.monotonic()-start,3),'gracefulStopRequested':stop,'automaticStop':automatic,'productAgent':args.get('product',False)},indent=2)+'\n',encoding='utf8')
print('server_runtime_exit',p.returncode,'gracefulStopRequested',stop,'autoStop',automatic,'elapsedSeconds',round(time.monotonic()-start,1))
