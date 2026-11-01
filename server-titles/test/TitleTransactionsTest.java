import dev.pashacat.titles.TitleTransactions;
import dev.pashacat.titles.TitleTransactions.*;
import java.util.*;
import java.util.concurrent.*;

class TitleTransactionsTest {
    static final UUID PLAYER=UUID.randomUUID();
    static final Title MONEY=new Title("money","Торговец","&e[Торговец]",Kind.MONEY,5000,"GOLD_INGOT",11);
    static final Title TIME=new Title("time","Наблюдатель","&a[Наблюдатель]",Kind.PLAYTIME,72000,"CLOCK",13);
    static final Title KILLS=new Title("kills","Дуэлянт","&c[Дуэлянт]",Kind.KILLS,10,"IRON_SWORD",15);
    static class Profile implements TitleTransactions.Profile {
        Set<String> owned=new HashSet<>();String selected="none";Queue<CompletableFuture<Void>> saves=new ArrayDeque<>();
        record Snapshot(Set<String> owned,String selected){}
        public boolean owns(Title t){return owned.contains(t.id());}public String selected(){return selected;}
        public Object checkpoint(){return new Snapshot(new HashSet<>(owned),selected);}
        public void select(Title t){selected=t==null?"none":t.id();if(t!=null)owned.add(t.id());}
        public void restore(Object value){Snapshot s=(Snapshot)value;owned=s.owned();selected=s.selected();}
        public CompletableFuture<Void> save(){return saves.isEmpty()?CompletableFuture.completedFuture(null):saves.remove();}
    }
    static class Wallet implements Account {
        long balance=10000;int charges,refunds;
        public boolean withdraw(long price){if(balance<price)return false;balance-=price;charges++;return true;}
        public boolean refund(long price){balance+=price;refunds++;return true;}
    }
    static void equal(Object a,Object b){if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
    static CompletableFuture<Result> run(TitleTransactions flow,Profile p,Wallet w,Title t,long ticks,long kills){return flow.select(PLAYER,p,w,t,ticks,kills,Runnable::run,error->{});}
    public static void main(String[] args){
        TitleTransactions flow=new TitleTransactions();Profile p=new Profile();Wallet w=new Wallet();
        equal(run(flow,p,w,TIME,71999,0).join(),Result.REQUIREMENT);equal(p.selected,"none");equal(w.balance,10000L);
        equal(run(flow,p,w,TIME,72000,0).join(),Result.SELECTED);equal(w.charges,0);
        equal(run(flow,p,w,KILLS,0,9).join(),Result.REQUIREMENT);equal(run(flow,p,w,KILLS,0,10).join(),Result.SELECTED);
        equal(run(flow,p,w,TIME,0,0).join(),Result.SELECTED); // previously earned stays unlocked
        equal(run(flow,p,w,null,0,0).join(),Result.CLEARED);equal(p.owned.size(),2);
        w.balance=4999;equal(run(flow,p,w,MONEY,0,0).join(),Result.NO_MONEY);equal(p.owns(MONEY),false);
        w.balance=10000;CompletableFuture<Void> pending=new CompletableFuture<>();p.saves.add(pending);
        CompletableFuture<Result> first=run(flow,p,w,MONEY,0,0);
        equal(run(flow,p,w,MONEY,0,0).join(),Result.BUSY);equal(w.charges,1);equal(first.isDone(),false);
        pending.complete(null);equal(first.join(),Result.SELECTED);equal(w.balance,5000L);
        equal(run(flow,p,w,MONEY,0,0).join(),Result.ALREADY_SELECTED);
        run(flow,p,w,TIME,0,0).join();equal(run(flow,p,w,MONEY,0,0).join(),Result.SELECTED);equal(w.charges,1);
        Profile failing=new Profile();Wallet refund=new Wallet();failing.saves.add(CompletableFuture.failedFuture(new IllegalStateException("storage failure")));
        equal(run(flow,failing,refund,MONEY,0,0).join(),Result.STORAGE_ERROR);equal(refund.balance,10000L);equal(refund.refunds,1);equal(failing.owned.size(),0);equal(failing.selected,"none");equal(flow.busy(PLAYER),false);
        System.out.println("PASS: time/kill boundaries, permanent unlocks, insufficient funds, one charge on duplicate click, free switching, clear preserves collection, failed save rolls back and refunds.");
    }
}
