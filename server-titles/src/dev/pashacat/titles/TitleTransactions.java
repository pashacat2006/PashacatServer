package dev.pashacat.titles;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Purchase rules are independent of Bukkit so failure and duplicate-click paths can be tested. */
public final class TitleTransactions {
    public enum Kind { MONEY, PLAYTIME, KILLS }
    public record Title(String id,String name,String suffix,Kind kind,long requirement,String material,int slot) {
        public Title {
            if(!id.matches("[a-z0-9_]+")||requirement<=0)throw new IllegalArgumentException("Invalid title "+id);
        }
        public String group(){return "stitle_"+id;}
        public String permission(){return "servertitles.owned."+id;}
    }
    public enum Result { SELECTED, ALREADY_SELECTED, CLEARED, BUSY, REQUIREMENT, NO_MONEY, STORAGE_ERROR }
    public interface Profile {
        boolean owns(Title title);
        String selected();
        Object checkpoint();
        void select(Title title);
        void restore(Object checkpoint);
        CompletableFuture<Void> save();
    }
    public interface Account { boolean withdraw(long amount); boolean refund(long amount); }
    private final Set<UUID> pending=ConcurrentHashMap.newKeySet();
    public boolean busy(UUID uuid){return pending.contains(uuid);}
    public CompletableFuture<Result> select(UUID uuid,Profile profile,Account account,Title title,long playedTicks,long kills,Executor callback,Consumer<Throwable> log){
        if(!pending.add(uuid))return CompletableFuture.completedFuture(Result.BUSY);
        boolean owned=title!=null&&profile.owns(title);
        if(title!=null&&title.id().equals(profile.selected()))return finish(uuid,Result.ALREADY_SELECTED);
        if(title!=null&&!owned&&((title.kind()==Kind.PLAYTIME&&playedTicks<title.requirement())||(title.kind()==Kind.KILLS&&kills<title.requirement())))return finish(uuid,Result.REQUIREMENT);
        long price=title!=null&&!owned&&title.kind()==Kind.MONEY?title.requirement():0;
        Object before=profile.checkpoint();
        try {
            if(price>0&&!account.withdraw(price))return finish(uuid,Result.NO_MONEY);
        } catch(Throwable error){pending.remove(uuid);log.accept(error);return CompletableFuture.completedFuture(Result.STORAGE_ERROR);}
        CompletableFuture<Void> save;
        try {profile.select(title);save=profile.save();}
        catch(Throwable error){save=CompletableFuture.failedFuture(error);}
        CompletableFuture<Result> result=new CompletableFuture<>();
        save.whenComplete((ignored,error)->callback.execute(()->{
            if(error==null){pending.remove(uuid);result.complete(title==null?Result.CLEARED:Result.SELECTED);return;}
            log.accept(error);
            try {
                profile.restore(before);
                if(price>0&&!account.refund(price))log.accept(new IllegalStateException("Title refund failed for "+uuid+": "+price));
                profile.save().whenComplete((nothing,rollbackError)->{if(rollbackError!=null)log.accept(rollbackError);pending.remove(uuid);result.complete(Result.STORAGE_ERROR);});
            }catch(Throwable rollbackError){log.accept(rollbackError);pending.remove(uuid);result.complete(Result.STORAGE_ERROR);}
        }));
        return result;
    }
    private CompletableFuture<Result> finish(UUID uuid,Result result){pending.remove(uuid);return CompletableFuture.completedFuture(result);}
}
