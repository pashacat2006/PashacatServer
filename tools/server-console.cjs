// Starts the server with an interactive console while hiding configured secrets in tool output.
const fs=require('node:fs'),cp=require('node:child_process'),readline=require('node:readline'),net=require('node:net');
const config=fs.readFileSync('plugins/FlectonePulse/integration.yml','utf8');
const secrets=[...config.matchAll(/^\s*(?:token|password|auth_key):\s*"([^"\r\n]+)"/gm)].map(m=>m[1]).filter(v=>v.length>5);
const sanitize=s=>{for(const secret of secrets)s=s.split(secret).join('<hidden>');return s.replace(/bot\d+:[\w-]+/g,'bot<hidden>');};
const probe=net.createServer();
probe.once('error',()=>{console.error('Server port 25565 is already in use; refusing a second instance.');process.exit(1);});
probe.listen({host:'0.0.0.0',port:25565,exclusive:true},()=>probe.close(start));
function start(){
const server=cp.spawn('C:/Program Files/Java/jdk-21.0.11/bin/java.exe',['-Dfile.encoding=UTF-8','-Dstdout.encoding=UTF-8','-Dstderr.encoding=UTF-8','-Dterminal.jline=false','-Dterminal.ansi=false','-Xms1G','-Xmx4G','-jar','server.jar','nogui'],{cwd:process.cwd(),stdio:['pipe','pipe','pipe'],windowsHide:true});
for(const stream of [server.stdout,server.stderr])readline.createInterface({input:stream}).on('line',line=>process.stdout.write(sanitize(line)+'\n'));
process.stdin.on('data',data=>server.stdin.write(data));
server.on('exit',code=>{console.log('Server process exited: '+code);process.exit(code??1);});
server.on('error',err=>{console.error(sanitize(err.message));process.exit(1);});
}
