const fs = require('node:fs');
const zlib = require('node:zlib');
function parse(buf) {
  let p=0;
  const str=()=>{const n=buf.readUInt16BE(p);p+=2;const s=buf.toString('utf8',p,p+n);p+=n;return s;};
  function value(t){let n,v;
    switch(t){
      case 1:v=buf.readInt8(p);p++;return v;
      case 2:v=buf.readInt16BE(p);p+=2;return v;
      case 3:v=buf.readInt32BE(p);p+=4;return v;
      case 4:v=buf.readBigInt64BE(p);p+=8;return v;
      case 5:v=buf.readFloatBE(p);p+=4;return v;
      case 6:v=buf.readDoubleBE(p);p+=8;return v;
      case 7:n=buf.readInt32BE(p);p+=4;v=buf.subarray(p,p+n);p+=n;return v;
      case 8:return str();
      case 9:{const type=buf[p++];n=buf.readInt32BE(p);p+=4;return {type,items:Array.from({length:n},()=>value(type))};}
      case 10:v={};while((t=buf[p++])!==0){const name=str();v[name]={t,v:value(t)};}return v;
      case 11:case 12:n=buf.readInt32BE(p);p+=4;return Array.from({length:n},()=>{const v=t===11?buf.readInt32BE(p):buf.readBigInt64BE(p);p+=t===11?4:8;return v;});
      default:throw Error('NBT tag '+t);
    }
  }
  const t=buf[p++],name=str(),v=value(t);if(p!==buf.length)throw Error('Trailing NBT bytes');return {t,name,v};
}
function encode(root){const parts=[];const num=(n,method,size)=>{const b=Buffer.alloc(size);b[method](n);parts.push(b);};const str=s=>{const b=Buffer.from(s);num(b.length,'writeUInt16BE',2);parts.push(b);};
  function value(t,v){switch(t){
    case 1:num(v,'writeInt8',1);break;case 2:num(v,'writeInt16BE',2);break;case 3:num(v,'writeInt32BE',4);break;case 4:num(v,'writeBigInt64BE',8);break;case 5:num(v,'writeFloatBE',4);break;case 6:num(v,'writeDoubleBE',8);break;
    case 7:num(v.length,'writeInt32BE',4);parts.push(v);break;case 8:str(v);break;
    case 9:num(v.type,'writeUInt8',1);num(v.items.length,'writeInt32BE',4);v.items.forEach(x=>value(v.type,x));break;
    case 10:for(const [name,tag] of Object.entries(v)){num(tag.t,'writeUInt8',1);str(name);value(tag.t,tag.v);}num(0,'writeUInt8',1);break;
    case 11:case 12:num(v.length,'writeInt32BE',4);v.forEach(x=>num(x,t===11?'writeInt32BE':'writeBigInt64BE',t===11?4:8));break;
  }}num(root.t,'writeUInt8',1);str(root.name);value(root.t,root.v);return Buffer.concat(parts);
}
function readRegion(path){const bytes=fs.readFileSync(path),chunks=new Map();for(let i=0;i<1024;i++){const off=bytes.readUIntBE(i*4,3)*4096;if(!off)continue;const len=bytes.readUInt32BE(off),type=bytes[off+4];if(type!==2)throw Error('Unexpected compression '+type);const data=zlib.inflateSync(bytes.subarray(off+5,off+4+len));const root=parse(data);if(!encode(root).equals(data))throw Error('NBT roundtrip failed');chunks.set(i,{root,raw:bytes.subarray(off,off+Math.ceil((len+4)/4096)*4096)});}return {bytes,chunks};}
function section(c,y){return c.v.sections.v.items.find(s=>s.Y.v===Math.floor(y/16));}
function ids(s){const bs=s.block_states.v,p=bs.palette.v.items,bits=Math.max(4,Math.ceil(Math.log2(p.length))),per=Math.floor(64/bits),mask=(1n<<BigInt(bits))-1n;return Array.from({length:4096},(_,i)=>bs.data?Number((BigInt.asUintN(64,bs.data.v[Math.floor(i/per)])>>BigInt(i%per*bits))&mask):0);}
function block(c,x,y,z){const s=section(c,y);if(!s?.block_states)return 'minecraft:air';const a=ids(s);return s.block_states.v.palette.v.items[a[((y&15)*256)+((z&15)*16)+(x&15)]].Name.v;}
module.exports={parse,encode,readRegion,ids,section,block};
if(require.main===module){const r=readRegion('world/region/r.-1.0.mca');let points=[];for(const {root:c} of r.chunks.values()){const cx=c.v.xPos.v,cz=c.v.zPos.v;if(cx< -12||cx> -2||cz< -4||cz>10)continue;for(const s of c.v.sections.v.items){if(!s.block_states)continue;const p=s.block_states.v.palette.v.items,ore=p.map((v,i)=>v.Name.v==='minecraft:iron_ore'?i:-1).filter(i=>i>=0);if(!ore.length)continue;ids(s).forEach((id,i)=>{if(ore.includes(id)){const x=cx*16+(i&15),y=s.Y.v*16+(i>>8),z=cz*16+((i>>4)&15);if(y>=0&&y<80)points.push([x,y,z]);}});}}
const byY={};for(const [x,y,z] of points){const a=byY[y]??={count:0,minX:x,maxX:x,minZ:z,maxZ:z};a.count++;a.minX=Math.min(a.minX,x);a.maxX=Math.max(a.maxX,x);a.minZ=Math.min(a.minZ,z);a.maxZ=Math.max(a.maxZ,z);}console.log(JSON.stringify(byY,null,2));fs.writeFileSync('tools/iron-ore-scan.json',JSON.stringify(points));}
