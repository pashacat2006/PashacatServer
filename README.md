# PashacatServer

Конфиги и плагины Minecraft-сервера Pashacat на Folia 1.21.4.

## Чего здесь нет

- миров (`world`, `world_nether`, `world_the_end`);
- баз данных (`*.db`, `*.mv.db` и другие) — LuckPerms, PlayerPoints, DonateCase и остальные создадут их заново;
- данных игроков: `usercache.json`, `ops.json`, `whitelist.json`, баны, `Essentials/userdata`, `MythicMobs/data/players` и подобное;
- ядра `server.jar` и библиотек, которые Folia и плагины скачивают сами.

## Пароли

При коммите git пропускает файлы `*.yml`, `*.yaml`, `*.properties` и `*.conf` через фильтр `.gitfilters/redact.sed`: значения `password`, `token`, `secret`, `api-key` и `rcon.password` попадают в репозиторий пустыми, а на диске остаются настоящими.

После клонирования подключи фильтр, иначе git откажется коммитить эти файлы:

```
git config filter.redact.clean "sed -E -f .gitfilters/redact.sed"
git config filter.redact.smudge cat
git config filter.redact.required true
```

И впиши свои значения: `rcon.password` в `server.properties`, ключ в `plugins/Updater/config.yml` и пароли баз, если перейдёшь на MySQL.

## Запуск

1. Скачай Folia 1.21.4 и положи как `server.jar`.
2. `java -jar server.jar` (нужна Java 21).
