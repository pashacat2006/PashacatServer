// Offline only. Back up the region and WorldGuard configuration before writing.
const fs=require('node:fs'),zlib=require('node:zlib'),cp=require('node:child_process');
const {readRegion,encode,ids,section}=require('./world-nbt.cjs');
if(cp.execFileSync('tasklist',['/FI','IMAGENAME eq java.exe','/FO','CSV'],{encoding:'utf8'}).includes('java.exe'))throw Error('Stop Java before editing the world');
const path='world/region/r.-1.0.mca',backup='backups/2026-11-01-spawn-house';
fs.mkdirSync(backup,{recursive:true});if(fs.existsSync(backup+'/r.-1.0.mca'))throw Error('Backup already exists');
const original=readRegion(path),dirty=new Map(),changed=new Map(),entities=[];
const tag=(t,v)=>({t,v}),str=v=>tag(8,v),int=v=>tag(3,v),short=v=>tag(2,v);
function state(name,props={}){const v={Name:str('minecraft:'+name)};if(Object.keys(props).length)v.Properties=tag(10,Object.fromEntries(Object.entries(props).map(([k,v])=>[k,str(String(v))])));return v;}
function locate(x,y,z){const index=(Math.floor(x/16)&31)+(Math.floor(z/16)&31)*32,c=original.chunks.get(index)?.root;if(!c)throw Error('Missing chunk');const s=section(c,y);if(!s?.block_states)throw Error('Missing section');const key=index+':'+s.Y.v;if(!dirty.has(key))dirty.set(key,{index,c,s,a:ids(s)});return {entry:dirty.get(key),i:(y&15)*256+(z&15)*16+(x&15)};}
function set(x,y,z,name,props){if(x< -117||x> -88||z<71||z>101||y<20||y>37)throw Error('Outside box');const {entry:e,i}=locate(x,y,z),p=e.s.block_states.v.palette.v.items,v=state(name,props);let id=p.findIndex(n=>JSON.stringify(n)===JSON.stringify(v));if(id<0){id=p.length;p.push(v);}e.a[i]=id;changed.set([x,y,z].join(','),{x,y,z,name,props});}
function fill(x1,y1,z1,x2,y2,z2,name,props){for(let x=x1;x<=x2;x++)for(let y=y1;y<=y2;y++)for(let z=z1;z<=z2;z++)set(x,y,z,name,props);}
const noise=(x,y,z)=>Math.abs((x*7349+y*139+z*9157)%101);
// A damp yard, gravel approach, and a masonry foundation.
for(let x=-116;x<=-89;x++)for(let z=72;z<=100;z++)set(x,20,z,noise(x,20,z)<35?'moss_block':noise(x,20,z)<65?'coarse_dirt':'gravel');
fill(-112,21,77,-94,21,95,'mossy_stone_bricks');
for(let x=-111;x<=-95;x++)for(let z=78;z<=94;z++)set(x,21,z,noise(x,21,z)<22?'mossy_stone_bricks':'spruce_planks');
// Timber frame with weathered plaster infill, two floors.
for(let y=22;y<=30;y++)for(let x=-111;x<=-95;x++)for(let z=78;z<=94;z++)if(x===-111||x===-95||z===78||z===94){const beam=(x===-111||x===-103||x===-95)&&(z===78||z===86||z===94);set(x,y,z,beam?'dark_oak_log':noise(x,y,z)<25?'cracked_stone_bricks':'mud_bricks',beam?{axis:'y'}:undefined);}
for(const y of [26,30]){fill(-111,y,78,-95,y,78,'dark_oak_log',{axis:'x'});fill(-111,y,94,-95,y,94,'dark_oak_log',{axis:'x'});fill(-111,y,79,-111,y,93,'dark_oak_log',{axis:'z'});fill(-95,y,79,-95,y,93,'dark_oak_log',{axis:'z'});}
fill(-110,26,79,-96,26,93,'spruce_planks');
// Broken windows and a doorless entry.
for(const y of [23,28])for(const x of [-108,-98])for(const z of [78,94]){fill(x,y,z,x+1,y+1,z,'air');set(x,y,z,'iron_bars',{east:'true',west:'true',north:'false',south:'false',waterlogged:'false'});}
for(const y of [23,28])for(const z of [82,89])for(const x of [-111,-95]){fill(x,y,z,x,y+1,z+1,'air');set(x,y,z,'iron_bars',{north:'true',south:'true',east:'false',west:'false',waterlogged:'false'});}
fill(-104,22,78,-102,24,78,'air');fill(-104,21,73,-102,21,76,'mossy_cobblestone');
// Gabled roof, jagged holes, and a ruined chimney.
for(let x=-113;x<=-93;x++){const y=31+Math.min(x+113,-93-x);if(y>36)continue;for(let z=76;z<=96;z++){if((z>=89&&z<=92&&x>=-110&&x<=-107)||(z>=79&&z<=81&&x>=-98&&x<=-96))continue;set(x,y,z,'dark_oak_stairs',{facing:x< -103?'east':'west',half:'bottom',shape:'straight',waterlogged:'false'});}}
fill(-106,36,76,-100,36,96,'dark_oak_slab',{type:'bottom',waterlogged:'false'});
for(const z of [78,94])for(let x=-110;x<=-96;x++)for(let y=31;y<=35;y++)if(y<=36-Math.abs(x+103))set(x,y,z,noise(x,y,z)<30?'spruce_planks':'dark_oak_planks');
fill(-99,22,91,-97,34,93,'cracked_stone_bricks');fill(-98,22,91,-98,23,92,'air');set(-98,22,92,'campfire',{facing:'north',lit:'false',signal_fire:'false',waterlogged:'false'});
// Staircase, a collapsed patch of floor, furniture, and webs.
for(let step=0;step<5;step++){fill(-109,22,81+step,-108,21+step,81+step,'spruce_planks');fill(-109,22+step,81+step,-108,22+step,81+step,'spruce_stairs',{facing:'south',half:'bottom',shape:'straight',waterlogged:'false'});fill(-109,23+step,81+step,-108,28+step,81+step,'air');}
fill(-101,26,85,-99,26,87,'air');set(-101,22,85,'spruce_planks');set(-100,22,86,'spruce_slab',{type:'bottom',waterlogged:'false'});
fill(-104,22,87,-102,22,87,'dark_oak_fence',{north:'false',south:'false',east:'true',west:'true',waterlogged:'false'});fill(-104,23,87,-102,23,87,'spruce_pressure_plate',{powered:'false'});
set(-106,22,92,'crafting_table');fill(-110,22,90,-110,24,92,'bookshelf');set(-106,27,92,'barrel',{facing:'up',open:'false'});
for(const [x,y,z] of [[-110,25,79],[-96,25,93],[-109,29,93],[-96,29,79],[-103,25,94],[-98,30,88],[-108,23,92],[-110,27,90]])set(x,y,z,'cobweb');
for(const [x,z] of [[-114,74],[-91,97],[-115,96]]){set(x,21,z,'mossy_cobblestone_wall',{up:'true',north:'none',south:'none',east:'none',west:'none',waterlogged:'false'});set(x,22,z,'soul_lantern',{hanging:'false',waterlogged:'false'});}
function entity(x,y,z,id,extra){entities.push({id:str('minecraft:'+id),x:int(x),y:int(y),z:int(z),...extra});}
for(const [x,y,z,loot,seed] of [[-110,22,88,'abandoned_mineshaft',125n],[-96,22,80,'simple_dungeon',267n],[-110,27,92,'ruined_portal',984n]]){set(x,y,z,'chest',{facing:'east',type:'single',waterlogged:'false'});entity(x,y,z,'chest',{LootTable:str('minecraft:chests/'+loot),LootTableSeed:tag(4,seed)});}
for(const [x,y,z,mob] of [[-97,22,89,'zombie'],[-97,27,83,'skeleton']]){set(x,y,z,'spawner');entity(x,y,z,'mob_spawner',{Delay:short(200),MinSpawnDelay:short(400),MaxSpawnDelay:short(800),SpawnCount:short(2),MaxNearbyEntities:short(4),RequiredPlayerRange:short(12),SpawnRange:short(3),SpawnData:tag(10,{entity:tag(10,{id:str('minecraft:'+mob)})}),SpawnPotentials:tag(9,{type:10,items:[{weight:int(1),data:tag(10,{entity:tag(10,{id:str('minecraft:'+mob)})})}]})});}
const changedChunks=new Set();
for(const {index,c,s,a} of dirty.values()){const p=s.block_states.v.palette.v.items,bits=Math.max(4,Math.ceil(Math.log2(p.length))),per=Math.floor(64/bits),data=Array(Math.ceil(4096/per)).fill(0n);a.forEach((id,i)=>data[Math.floor(i/per)]|=BigInt(id)<<BigInt(i%per*bits));s.block_states.v.data=tag(12,data.map(v=>BigInt.asIntN(64,v)));delete s.BlockLight;delete s.SkyLight;c.v.isLightOn=tag(1,0);changedChunks.add(index);}
for(const index of changedChunks){const c=original.chunks.get(index).root;if(!c.v.block_entities)c.v.block_entities=tag(9,{type:10,items:[]});c.v.block_entities.v.type=10;c.v.block_entities.v.items=c.v.block_entities.v.items.filter(e=>!changed.has([e.x.v,e.y.v,e.z.v].join(',')));for(const e of entities)if((Math.floor(e.x.v/16)&31)+(Math.floor(e.z.v/16)&31)*32===index)c.v.block_entities.v.items.push(e);}
// Interior edits are below the existing roof: height maps remain valid.
const header=Buffer.from(original.bytes.subarray(0,8192)),parts=[header];let offset=2;
for(let i=0;i<1024;i++){const entry=original.chunks.get(i);if(!entry){header.writeUInt32BE(0,i*4);continue;}let data=entry.raw;if(changedChunks.has(i)){const zip=zlib.deflateSync(encode(entry.root));data=Buffer.alloc(Math.ceil((zip.length+5)/4096)*4096);data.writeUInt32BE(zip.length+1);data[4]=2;zip.copy(data,5);header.writeUInt32BE(Math.floor(Date.now()/1000),4096+i*4);}if(data.length/4096>255)throw Error('Oversize chunk');header.writeUIntBE(offset,i*4,3);header[i*4+3]=data.length/4096;parts.push(data);offset+=data.length/4096;}
const output=Buffer.concat(parts);fs.writeFileSync(path+'.house.tmp',output);const check=readRegion(path+'.house.tmp');for(const [i,entry] of original.chunks)if(!changedChunks.has(i)&&!check.chunks.get(i).raw.equals(entry.raw))throw Error('Unrelated chunk modified');
// Verify every edited block after rereading serialized region data.
const cache=new Map();for(const p of changed.values()){const i=(Math.floor(p.x/16)&31)+(Math.floor(p.z/16)&31)*32,c=check.chunks.get(i).root,s=section(c,p.y),key=i+':'+s.Y.v;if(!cache.has(key))cache.set(key,ids(s));const actual=s.block_states.v.palette.v.items[cache.get(key)[(p.y&15)*256+(p.z&15)*16+(p.x&15)]].Name.v;if(actual!=='minecraft:'+p.name)throw Error('Block verification failed');}
const wg='plugins/WorldGuard/worlds/world/regions.yml',wgOriginal=fs.readFileSync(wg,'utf8');if(wgOriginal.includes('    abandoned_house:'))throw Error('Region exists');
fs.copyFileSync(path,backup+'/r.-1.0.mca');fs.copyFileSync(wg,backup+'/regions.yml');
fs.renameSync(path+'.house.tmp',path);
fs.writeFileSync(wg,wgOriginal.replace('regions:',`regions:
    abandoned_house:
        min: {x: -117, y: 20, z: 71}
        max: {x: -88, y: 37, z: 101}
        members: {}
        owners: {}
        flags: {mob-spawning: allow, chest-access: allow, use: allow, pvp: deny, block-break: deny, block-place: deny}
        type: cuboid
        priority: 5`));
fs.writeFileSync(backup+'/house-manifest.json',JSON.stringify({bounds:{min:[-117,20,71],max:[-88,37,101]},teleport:[-103,22,75],changedBlocks:changed.size,changedChunks:[...changedChunks],chests:3,spawners:['zombie','skeleton']},null,2));
console.log(JSON.stringify({changedBlocks:changed.size,chunks:changedChunks.size,chests:3,spawners:2,backup,teleport:'/tp @s -103 22 75'}));
