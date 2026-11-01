const fs=require('node:fs');
// Per-item prices in the existing Vault currency. Sale prices never exceed purchase prices.
const categories=[
  {id:'Blocks',name:'Блоки и дерево',icon:'GRASS_BLOCK',slot:11,color:'&a',items:[
    ['COBBLESTONE','Булыжник',8,1],['STONE','Камень',12,3],['STONE_BRICKS','Каменные кирпичи',16,3],
    ['DIRT','Земля',8,1],['SAND','Песок',12,1],['GRAVEL','Гравий',12,1],['GLASS','Стекло',24,3],
    ['OAK_LOG','Дубовое бревно',24,2],['SPRUCE_LOG','Еловое бревно',24,2],['BIRCH_LOG','Берёзовое бревно',24,2],
    ['DARK_OAK_LOG','Бревно тёмного дуба',32,2],['CHERRY_LOG','Вишнёвое бревно',40,3],
    ['OAK_PLANKS','Дубовые доски',8,0.5],['SPRUCE_PLANKS','Еловые доски',8,0.5],
    ['OBSIDIAN','Обсидиан',600,130],['NETHERRACK','Незерак',8,0.5],['WHITE_WOOL','Белая шерсть',100,20],
    ['BRICKS','Кирпичи',100,21]
  ]},
  {id:'Resources',name:'Ресурсы',icon:'DIAMOND',slot:13,color:'&b',items:[
    ['COAL','Уголь',80,15],['CHARCOAL','Древесный уголь',40,8],['IRON_INGOT','Железный слиток',180,22],
    ['GOLD_INGOT','Золотой слиток',600,105],['COPPER_INGOT','Медный слиток',60,4],
    ['DIAMOND','Алмаз',2400,200],['EMERALD','Изумруд',1800,100],['LAPIS_LAZULI','Лазурит',200,15],
    ['REDSTONE','Редстоун',120,32],['QUARTZ','Незер-кварц',80,8],['AMETHYST_SHARD','Осколок аметиста',60,4],
    ['GLOWSTONE_DUST','Светокаменная пыль',80,10]
  ]},
  {id:'Farming',name:'Фермерство',icon:'WHEAT',slot:15,color:'&e',items:[
    ['WHEAT_SEEDS','Семена пшеницы',10,2],['BEETROOT_SEEDS','Семена свёклы',10,1],
    ['WHEAT','Пшеница',40,9],['CARROT','Морковь',30,3],['POTATO','Картофель',30,3],
    ['BEETROOT','Свёкла',30,3],['SUGAR_CANE','Сахарный тростник',60,10],['CACTUS','Кактус',80,8],
    ['PUMPKIN','Тыква',200,40],['MELON_SLICE','Ломтик арбуза',16,1],
    ['OAK_SAPLING','Саженец дуба',32,2],['SPRUCE_SAPLING','Саженец ели',32,2],
    ['BONE_MEAL','Костная мука',20,-1],['NETHER_WART','Нарост Незера',80,5]
  ]},
  {id:'Food',name:'Еда',icon:'COOKED_BEEF',slot:29,color:'&6',items:[
    ['BREAD','Хлеб',160,30],['COOKED_BEEF','Стейк',100,8],['COOKED_PORKCHOP','Жареная свинина',100,7],
    ['COOKED_CHICKEN','Жареная курятина',80,6],['BAKED_POTATO','Печёный картофель',60,6],
    ['APPLE','Яблоко',80,10],['GOLDEN_APPLE','Золотое яблоко',2400,100],
    ['COOKED_COD','Жареная треска',80,7],['COOKED_SALMON','Жареный лосось',100,8],
    ['PUMPKIN_PIE','Тыквенный пирог',280,20]
  ]},
  {id:'Drops',name:'Добыча с мобов',icon:'BONE',slot:31,color:'&c',items:[
    ['ROTTEN_FLESH','Гнилая плоть',16,1],['BONE','Кость',40,2],['STRING','Нить',40,5],
    ['SPIDER_EYE','Паучий глаз',60,4],['GUNPOWDER','Порох',150,20],['LEATHER','Кожа',80,10],
    ['FEATHER','Перо',30,3],['SLIME_BALL','Сгусток слизи',240,40],
    ['ENDER_PEARL','Эндер-жемчуг',1000,40],['BLAZE_ROD','Огненный стержень',500,25]
  ]},
  {id:'Supplies',name:'Полезные вещи',icon:'IRON_PICKAXE',slot:33,color:'&d',items:[
    ['TORCH','Факел',12,-1],['CHEST','Сундук',80,-1],['CRAFTING_TABLE','Верстак',40,-1],
    ['FURNACE','Печь',100,-1],['BUCKET','Ведро',600,-1],['WATER_BUCKET','Ведро воды',800,-1],
    ['IRON_PICKAXE','Железная кирка',800,-1],['IRON_AXE','Железный топор',800,-1],
    ['IRON_SHOVEL','Железная лопата',350,-1],['IRON_SWORD','Железный меч',600,-1],
    ['SHEARS','Ножницы',500,-1],['FISHING_ROD','Удочка',250,-1],['BOW','Лук',400,-1],
    ['ARROW','Стрела',24,3.5],['WHITE_BED','Белая кровать',400,-1],['BOOK','Книга',200,45]
  ]}
];
const root='plugins/EconomyShopGUI';
fs.mkdirSync(root+'/sections',{recursive:true});fs.mkdirSync(root+'/shops',{recursive:true});
const quote=s=>JSON.stringify(s);
let total=0;const materials=new Set();
for(const category of categories){
  let section=`enable: true\nslot: ${category.slot}\ntitle: ${quote('&8Магазин · '+category.name)}\nhidden: false\nsub-section: false\neconomy: VAULT\nfill-item:\n  material: GRAY_STAINED_GLASS_PANE\n  name: ' '\nnav-bar:\n  mode: INHERIT\nitem:\n  material: ${category.icon}\n  name: ${quote(category.color+'&l'+category.name)}\n  lore:\n    - ${quote('&7Товаров: &f'+category.items.length)}\n    - '&aЛКМ по товару — купить'\n    - '&6ПКМ по товару — продать'\n    - '&7Нажмите, чтобы открыть'\n`;
  fs.writeFileSync(`${root}/sections/${category.id}.yml`,section);
  let shop='pages:\n  page1:\n    gui-rows: 6\n    items:\n';
  const slots=[10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34];
  category.items.forEach(([material,name,buy,sell],i)=>{
    if(materials.has(material))throw Error('Duplicate '+material);materials.add(material);
    if(!(buy>0&&(sell===-1||(sell>=0&&sell<buy))))throw Error('Invalid price '+material);
    shop+=`      '${i+1}':\n        material: ${material}\n        displayname: ${quote('&f'+name)}\n        slot: ${slots[i]}\n        buy: ${buy}\n        sell: ${sell}\n`;
    total++;
  });
  fs.writeFileSync(`${root}/shops/${category.id}.yml`,shop);
}
fs.writeFileSync('tools/shop-catalog.json',JSON.stringify(categories,null,2));
console.log(`Created ${categories.length} Russian categories with ${total} items.`);
