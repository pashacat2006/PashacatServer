import java.util.*;
import java.util.logging.Level;
import net.luckperms.api.*;
import net.luckperms.api.node.types.*;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.plugin.java.JavaPlugin;

/** Temporary integration probe, removed from FoliaUpdater after execution. */
public final class TitleIntegrationProbe extends JavaPlugin {
    @Override public void onEnable(){
        getServer().dispatchCommand(getServer().getConsoleSender(),"lp reloadconfig");
        getServer().getGlobalRegionScheduler().runDelayed(this,task->verify(),20);
    }
    private void verify(){
        var lp=LuckPermsProvider.get();
        for(var online:getServer().getOnlinePlayers())online.getScheduler().run(this,task->{
            lp.getContextManager().signalContextUpdate(online);
            var loaded=lp.getUserManager().getUser(online.getUniqueId());
            if(loaded!=null)loaded.getCachedData().invalidate();
            getLogger().info("LIVE SUFFIX AFTER REFRESH: "+lp.getPlayerAdapter(org.bukkit.entity.Player.class).getMetaData(online).getSuffix());
        },null);
        lp.getUserManager().loadUser(UUID.randomUUID()).whenComplete((user,error)->{
            try {
                if(error!=null)throw new IllegalStateException(error);
                user.data().add(InheritanceNode.builder("default").build());
                user.data().add(SuffixNode.builder("&7[Личный]",20).build());
                String before=user.getCachedData().getMetaData(QueryOptions.nonContextual()).getSuffix();
                var group=Objects.requireNonNull(lp.getGroupManager().getGroup("stitle_money_trader"));
                String title=group.getNodes().stream().filter(n->n instanceof SuffixNode).map(n->((SuffixNode)n).getMetaValue()).findFirst().orElseThrow();
                user.data().add(InheritanceNode.builder(group).build());
                String selected=user.getCachedData().getMetaData(QueryOptions.nonContextual()).getSuffix();
                if(!Objects.equals(selected,title))throw new IllegalStateException("More than one visible suffix: "+selected);
                user.data().remove(InheritanceNode.builder(group).build());
                String after=user.getCachedData().getMetaData(QueryOptions.nonContextual()).getSuffix();
                if(!Objects.equals(before,after))throw new IllegalStateException("Clear changed old suffixes: "+after);
                getLogger().info("TITLE INTEGRATION PASS: exactly one visible title; clear restores original suffix; test user was not saved. Rendered: "+selected);
            }catch(Throwable failure){getLogger().log(Level.SEVERE,"TITLE INTEGRATION FAIL",failure);}
            finally {
                if(user!=null)lp.getUserManager().cleanupUser(user);
                getServer().getGlobalRegionScheduler().execute(this,()->getServer().getPluginManager().disablePlugin(this));
            }
        });
    }
}
