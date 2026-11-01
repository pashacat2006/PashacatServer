const fs=require('node:fs');
const backup='backups/2026-11-01-title-menu';fs.mkdirSync(backup,{recursive:true});
const rows=[
 ['money_trader','Торговец','&e', 'MONEY',5000,'GOLD_INGOT',11],
 ['money_rich','Богач','&6','MONEY',25000,'EMERALD',20],
 ['money_magnate','Магнат','&6','MONEY',100000,'GOLD_BLOCK',29],
 ['money_aristocrat','Аристократ','&d','MONEY',250000,'DIAMOND',38],
 ['money_emperor','Император','&6&l','MONEY',1000000,'NETHER_STAR',47],
 ['time_observer','Наблюдатель','&a','PLAYTIME',1,'CLOCK',13],
 ['time_regular','Постоялец','&2','PLAYTIME',5,'COMPASS',22],
 ['time_oldtimer','Старожил','&b','PLAYTIME',24,'BOOK',31],
 ['time_veteran','Ветеран','&9','PLAYTIME',72,'AMETHYST_SHARD',40],
 ['time_legend','Легенда сервера','&d&l','PLAYTIME',168,'NETHER_STAR',49],
 ['kills_duelist','Дуэлянт','&e','KILLS',10,'WOODEN_SWORD',15],
 ['kills_hunter','Охотник','&6','KILLS',50,'STONE_SWORD',24],
 ['kills_gladiator','Гладиатор','&c','KILLS',100,'IRON_SWORD',33],
 ['kills_conqueror','Завоеватель','&4','KILLS',250,'DIAMOND_SWORD',42],
 ['kills_executioner','Палач','&4&l','KILLS',500,'NETHERITE_SWORD',51]
];
function dump(v,indent=0){const pad=' '.repeat(indent);if(Array.isArray(v))return v.map(x=>typeof x==='object'?pad+'-\n'+dump(x,indent+2):pad+'- '+JSON.stringify(x)).join('\n')+'\n';return Object.entries(v).map(([k,x])=>{const key=/^[A-Za-z_][A-Za-z0-9_-]*$/.test(k)?k:JSON.stringify(k);return pad+key+':'+(x&&typeof x==='object'?'\n'+dump(x,indent+2).trimEnd():' '+JSON.stringify(x));}).join('\n')+'\n';}
const catalog={titles:{}};
const menu={menu_title:'&8Титулы &7· &6Коллекция',open_command:['title','titles','titul'],register_command:true,size:54,update_interval:1,
 open_requirement:{requirements:{ready:{type:'string equals',input:'%servertitles_ready%',output:'yes'}},deny_commands:['[message] &cТитулы ещё загружаются. Попробуйте позже.']},items:{}};
menu.items.money_header={material:'GOLD_INGOT',slot:2,display_name:'&6&lЗа монеты',lore:['&7Купите титул один раз.','&7Повторный выбор бесплатный.']};
menu.items.time_header={material:'CLOCK',slot:4,display_name:'&b&lЗа время',lore:['&7Награда за общий стаж на сервере.','&7Игровое время не списывается.']};
menu.items.kills_header={material:'IRON_SWORD',slot:6,display_name:'&c&lЗа PvP',lore:['&7Учитываются убийства игроков.','&7Убийства мобов не учитываются.','&7Статистика не списывается.'],item_flags:['HIDE_ATTRIBUTES']};
for(const [id,name,color,kind,amount,material,slot] of rows){
 const suffix=color+'['+name+']&r';
 catalog.titles[id]={name,suffix,kind,requirement:kind==='PLAYTIME'?amount*72000:amount,material,slot};
 const cost=kind==='MONEY'?'&7Цена открытия: &e'+amount.toLocaleString('ru-RU')+' монет':kind==='PLAYTIME'?'&7Нужно наиграть: &b'+amount+' ч.':'&7Нужно убийств игроков: &c'+amount;
 const progress=kind==='MONEY'?'&7Ваш баланс: &e%vault_eco_balance_formatted%':kind==='PLAYTIME'?'&7Ваш стаж: &b%servertitles_playtime%':'&7Ваши убийства: &c%servertitles_kills%';
 const item={material,slot,priority:2,update:true,display_name:color+name,lore:['',cost,progress,'','&7В чате: &fНик '+suffix,'&7Открытый титул остаётся навсегда.','','%servertitles_status_'+id+'%'],item_flags:['HIDE_ATTRIBUTES'],left_click_commands:['[close]','[player] servertitles select '+id],right_click_commands:['[close]','[player] servertitles select '+id]};
 menu.items[id+'_selected']={...item,priority:1,material:'LIME_STAINED_GLASS_PANE',display_name:'&a✔ '+name,view_requirement:{requirements:{selected:{type:'string equals',input:'%servertitles_selected_id%',output:id}}},left_click_commands:['[message] &aЭтот титул уже выбран.'],right_click_commands:['[message] &aЭтот титул уже выбран.']};
 menu.items[id]=item;
}
menu.items.clear={material:'MILK_BUCKET',slot:45,display_name:'&cСнять титул',lore:['&7Снимает выбранный титул.','&7Вернёт ваш обычный суффикс.','&7Полученные титулы сохраняются.'],left_click_commands:['[close]','[player] servertitles clear']};
menu.items.profile={material:'NAME_TAG',slot:48,display_name:'&eВаши достижения',update:true,lore:['&7Выбран: %servertitles_selected%','&7Стаж: &b%servertitles_playtime%','&7Убийств игроков: &c%servertitles_kills%','&7Монеты: &e%vault_eco_balance_formatted%']};
menu.items.help={material:'BOOK',slot:50,display_name:'&6Как работают титулы',lore:['&7После ника показан только выбранный титул.','&7Снять титул — вернуть обычный суффикс.','&7Полученные титулы меняются бесплатно.','&7Время и убийства не списываются.','&7Команды: &f/title &7или &f/titles']};
menu.items.close={material:'BARRIER',slot:53,display_name:'&cЗакрыть',left_click_commands:['[close]']};
const occupied=new Set(Object.values(menu.items).map(x=>x.slot));
menu.items.filler={material:'GRAY_STAINED_GLASS_PANE',slots:Array.from({length:54},(_,i)=>i).filter(i=>!occupied.has(i)),display_name:' '};
for(const [path,text] of [['server-titles/resources/config.yml',dump(catalog)],['plugins/DeluxeMenus/gui_menus/title.yml',dump(menu)]]){fs.mkdirSync(path.slice(0,path.lastIndexOf('/')),{recursive:true});if(fs.existsSync(path)&&!fs.existsSync(backup+'/'+path.split('/').pop()))fs.copyFileSync(path,backup+'/'+path.split('/').pop());fs.writeFileSync(path,text);}
const dmPath='plugins/DeluxeMenus/config.yml';let dm=fs.readFileSync(dmPath,'utf8');if(!/\n  Title:/.test(dm)){fs.copyFileSync(dmPath,backup+'/deluxemenus-config.yml');dm=dm.trimEnd()+'\n  Title:\n    file: title.yml\n';fs.writeFileSync(dmPath,dm);}
const lpPath='plugins/LuckPerms/config.yml';let lp=fs.readFileSync(lpPath,'utf8');const stacked='  suffix:\n    format:\n      - "highest_on_track_servertitles"\n      - "highest_own"\n      - "highest_not_on_track_servertitles"';const single='  suffix:\n    format:\n      - "highest"';
const normalized=lp.replace(/\r\n/g,'\n');if(normalized.includes(stacked)){if(!fs.existsSync(backup+'/luckperms-stacked-before-single.yml'))fs.copyFileSync(lpPath,backup+'/luckperms-stacked-before-single.yml');fs.writeFileSync(lpPath,normalized.replace(stacked,single));}else if(!normalized.includes(single))throw Error('Unexpected suffix stacking config');
const loaderPath='plugins/FoliaUpdater/config.yml';let loader=fs.readFileSync(loaderPath,'utf8');if(!loader.includes('  ServerTitles:')){fs.copyFileSync(loaderPath,backup+'/foliaupdater-config.yml');loader=loader.trimEnd()+'\n  ServerTitles: [Vault, LuckPerms, PlaceholderAPI, DeluxeMenus]\n';fs.writeFileSync(loaderPath,loader);}
console.log('Created Title menu with 15 titles; registered /title, /titles, /titul. Configured one visible suffix and FoliaUpdater load order.');
