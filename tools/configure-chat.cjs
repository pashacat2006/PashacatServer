const fs=require('node:fs');
const dir='backups/2026-11-01-spawn-house';fs.mkdirSync(dir,{recursive:true});
const path='plugins/FlectonePulse/message.yml';
if(fs.existsSync(dir+'/message.yml'))throw Error('Backup exists; inspect before rerunning');
const original=fs.readFileSync(path,'utf8');fs.copyFileSync(path,dir+'/message.yml');
let text=original;
for(const key of ['caps','flood','swear']){const re=new RegExp('(    '+key+':\\r?\\n      enable: )false');if(!re.test(text))throw Error('Unexpected '+key+' config');text=text.replace(re,'$1true');}
text=text.replace('max_repeated_symbols: 10','max_repeated_symbols: 5');
fs.writeFileSync(path,text);
console.log('Enabled caps (70%), flood (5 symbols / 2 words), swear (existing Russian/transliteration patterns).');
