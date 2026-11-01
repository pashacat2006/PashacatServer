const fs=require('node:fs'),net=require('node:net');
const text=fs.readFileSync('server.properties','utf8');
function property(key){const line=text.split(/\r?\n/).find(s=>s.startsWith(key+'='));if(!line)throw Error('Missing property: '+key);return line.slice(key.length+1).replace(/\\u([a-f0-9]{4})/gi,(_,hex)=>String.fromCharCode(parseInt(hex,16))).replace(/\\([:=#! ])/g,'$1');}
function command(cmd){return new Promise((resolve,reject)=>{
  const socket=net.createConnection({host:'127.0.0.1',port:Number(property('rcon.port'))});let pending=Buffer.alloc(0),done=false;
  const fail=err=>{if(done)return;done=true;socket.destroy();reject(err);};
  const send=(id,type,body)=>{const payload=Buffer.from(body,'utf8'),buf=Buffer.alloc(payload.length+14);buf.writeInt32LE(payload.length+10,0);buf.writeInt32LE(id,4);buf.writeInt32LE(type,8);payload.copy(buf,12);socket.write(buf);};
  socket.setTimeout(15000,()=>fail(Error('RCON timeout')));socket.on('error',fail);
  socket.on('connect',()=>send(1,3,property('rcon.password')));
  socket.on('data',chunk=>{pending=Buffer.concat([pending,chunk]);while(pending.length>=4){const len=pending.readInt32LE(0);if(len<10||len>4194304)return fail(Error('Invalid RCON response'));if(pending.length<len+4)return;const packet=pending.subarray(0,len+4);pending=pending.subarray(len+4);const id=packet.readInt32LE(4),type=packet.readInt32LE(8),body=packet.toString('utf8',12,len+2);if(id===-1)return fail(Error('RCON authentication rejected'));if(id===1&&type===2)send(2,2,cmd);if(id===2){done=true;socket.end();resolve(body);}}});
  socket.on('end',()=>{if(!done)fail(Error('RCON connection closed'));});
});}
module.exports={command};
if(require.main===module)(async()=>{for(const cmd of process.argv.slice(2)){console.log('> '+cmd);console.log(await command(cmd));}})().catch(err=>{console.error(err.message);process.exitCode=1;});
