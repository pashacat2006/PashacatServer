function RenameClan() {
    var clan = "%clans_name%"
    if (clan != "") {
        return clan
    }
    if (clan == "") {
        return "Отсутствует"
    }
}
RenameClan();