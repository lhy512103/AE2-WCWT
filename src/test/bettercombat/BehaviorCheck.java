import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Executes actual transformed BC methods and actual WCWT hotbar methods against game boundary fixtures. */
public class BehaviorCheck {
    private static Path root;
    private static void source(String name, String text) throws IOException {
        Path path = root.resolve("src/" + name.replace('.', '/') + ".java");
        Files.createDirectories(path.getParent()); Files.writeString(path,text);
    }
    private static void trim(Path input, Path output, Set<String> names) throws IOException {
        ClassNode node = new ClassNode(); new ClassReader(Files.readAllBytes(input)).accept(node,0);
        node.methods.removeIf(method -> !names.contains(method.name));
        node.nestMembers = null;
        ClassWriter writer = new ClassWriter(0); node.accept(writer);
        Files.createDirectories(output.getParent()); Files.write(output,writer.toByteArray());
    }
    public static void main(String[] args) throws Exception {
        root = Path.of(args[0]).resolve("behavior");
        Path out = root.resolve("classes"); Files.createDirectories(out);
        Path compiled = Path.of(args[1]);
        // Keep the original BC hand swap and attribute methods, plus the handler inserted by Mixin.
        ClassNode bc = new ClassNode(); new ClassReader(Files.readAllBytes(Path.of(args[0]).resolve("PlayerAttackHelper.transformed.class"))).accept(bc,0);
        Set<String> methods = new HashSet<>(Set.of("swapHandAttributes","setAttributesForOffHandAttack"));
        bc.methods.stream().filter(m -> m.name.contains("wcwt$setSwappedHand")).forEach(m -> methods.add(m.name));
        trim(Path.of(args[0]).resolve("PlayerAttackHelper.transformed.class"),out.resolve("net/bettercombat/logic/PlayerAttackHelper.class"),methods);
        trim(compiled.resolve("com/lhy/wcwt/helpers/WcwtToolkitHotbarState.class"),out.resolve("com/lhy/wcwt/helpers/WcwtToolkitHotbarState.class"),
                Set.of("state","getBar","getSlot","toolkitIndex","isToolkitSelected","isValidToolkitIndex","stackAt","writeCell","getSelectedToolkit","swapSelectedToolkitForAttributes"));
        Path bar = out.resolve("com/lhy/wcwt/helpers/WcwtToolkitHotbarState$Bar.class");
        Files.copy(compiled.resolve("com/lhy/wcwt/helpers/WcwtToolkitHotbarState$Bar.class"),bar,StandardCopyOption.REPLACE_EXISTING);
        Files.copy(compiled.resolve("com/lhy/wcwt/helpers/WcwtToolkitHand.class"),out.resolve("com/lhy/wcwt/helpers/WcwtToolkitHand.class"),StandardCopyOption.REPLACE_EXISTING);
        source("net.minecraft.world.item.ItemStack", """
            package net.minecraft.world.item;
            public class ItemStack { public static final ItemStack EMPTY = new ItemStack("empty",0);
                public final String id; public int count; public ItemStack(String id,int count){this.id=id;this.count=count;}
                public boolean isEmpty(){return count==0;} }
            """);
        source("net.minecraft.core.NonNullList", """
            package net.minecraft.core;
            public class NonNullList<T> extends java.util.ArrayList<T> { public NonNullList(int size,T initial){for(int i=0;i<size;i++)add(initial);} }
            """);
        source("net.minecraft.world.inventory.AbstractContainerMenu", "package net.minecraft.world.inventory; public class AbstractContainerMenu {}");
        source("net.minecraft.world.inventory.InventoryMenu", "package net.minecraft.world.inventory; public class InventoryMenu extends AbstractContainerMenu {}");
        source("net.minecraft.world.level.Level", "package net.minecraft.world.level; public class Level {public boolean client; public boolean isClientSide(){return client;}}");
        source("net.minecraft.server.level.ServerPlayerGameMode", "package net.minecraft.server.level; public class ServerPlayerGameMode {}");
        source("net.minecraft.server.level.ServerPlayer", "package net.minecraft.server.level; public class ServerPlayer extends net.minecraft.world.entity.player.Player {public ServerPlayerGameMode gameMode=new ServerPlayerGameMode();}");
        source("net.minecraft.world.entity.player.Inventory", """
            package net.minecraft.world.entity.player;
            import net.minecraft.core.NonNullList; import net.minecraft.world.item.ItemStack;
            public class Inventory {
                public NonNullList<ItemStack> items=new NonNullList<>(36,ItemStack.EMPTY),offhand=new NonNullList<>(1,ItemStack.EMPTY);
                public int selected=4; public final Player player; public Inventory(Player player){this.player=player;}
                public ItemStack getSelected(){return com.lhy.wcwt.helpers.WcwtToolkitHand.isOverrideActive(player)
                    ?com.lhy.wcwt.helpers.WcwtToolkitHotbarState.getSelectedToolkit(player):items.get(selected);}
            }
            """);
        source("net.minecraft.world.entity.ai.attributes.AttributeMap", """
            package net.minecraft.world.entity.ai.attributes;
            import com.google.common.collect.Multimap;
            public class AttributeMap {public final java.util.Set<String> active=new java.util.HashSet<>();
                public void removeAttributeModifiers(Multimap<String,String> modifiers){active.removeAll(modifiers.values());}
                public void addTransientAttributeModifiers(Multimap<String,String> modifiers){active.addAll(modifiers.values());}}
            """);
        source("net.bettercombat.utils.AttributeModifierHelper", """
            package net.bettercombat.utils;
            import com.google.common.collect.*; import net.minecraft.world.item.ItemStack;
            public class AttributeModifierHelper {public static Multimap<String,String> modifierMultimap(ItemStack stack){
                Multimap<String,String> map=HashMultimap.create();if(!stack.id.equals("empty"))map.put("damage",stack.id);return map;}}
            """);
        source("net.minecraft.world.entity.player.Player", """
            package net.minecraft.world.entity.player;
            import net.minecraft.world.item.ItemStack; import net.minecraft.world.level.Level;
            import net.minecraft.world.inventory.AbstractContainerMenu; import net.minecraft.world.entity.ai.attributes.AttributeMap;
            public class Player { public final Inventory inventory=new Inventory(this); public final Level level=new Level();
                public final net.minecraft.world.inventory.InventoryMenu inventoryMenu=new net.minecraft.world.inventory.InventoryMenu();
                public AbstractContainerMenu containerMenu=inventoryMenu;
                public boolean spectator; public final AttributeMap attributes=new AttributeMap();
                public final com.lhy.wcwt.helpers.WcwtToolkitPlayerState state=new com.lhy.wcwt.helpers.WcwtToolkitPlayerState();
                public Inventory getInventory(){return inventory;} public ItemStack getMainHandItem(){return inventory.getSelected();}
                public ItemStack getOffhandItem(){return inventory.offhand.get(0);} public boolean isSpectator(){return spectator;}
                public Level level(){return level;} public Object getData(java.util.function.Supplier<?> ignored){return state;}
                public AttributeMap getAttributes(){return attributes;}}
            """);
        source("appeng.api.inventories.InternalInventory", """
            package appeng.api.inventories; import net.minecraft.world.item.ItemStack;
            public interface InternalInventory {int size();ItemStack getStackInSlot(int index);void setItemDirect(int index,ItemStack stack);}
            """);
        source("appeng.util.inv.AppEngInternalInventory", """
            package appeng.util.inv; import net.minecraft.world.item.ItemStack;
            public class AppEngInternalInventory implements appeng.api.inventories.InternalInventory {
                public final ItemStack[] cells=new ItemStack[18]; public AppEngInternalInventory(){java.util.Arrays.fill(cells,ItemStack.EMPTY);}
                public int size(){return cells.length;}public ItemStack getStackInSlot(int index){return cells[index];}
                public void setItemDirect(int index,ItemStack stack){cells[index]=stack;}}
            """);
        source("com.lhy.wcwt.init.ModAttachments", "package com.lhy.wcwt.init; public class ModAttachments {public static final java.util.function.Supplier<Object> TOOLKIT_STATE=()->null;}");
        source("com.lhy.wcwt.helpers.WcwtToolkitPlayerState", """
            package com.lhy.wcwt.helpers;
            import net.minecraft.world.item.ItemStack;
            public class WcwtToolkitPlayerState {public boolean hasCard=true;
                public WcwtToolkitHotbarState.Bar page=WcwtToolkitHotbarState.Bar.LEFT;
                public final ItemStack[] clientItems=new ItemStack[18];
                public final appeng.util.inv.AppEngInternalInventory items=new appeng.util.inv.AppEngInternalInventory();
                public WcwtToolkitPlayerState(){java.util.Arrays.fill(clientItems,ItemStack.EMPTY);}}
            """);
        source("com.lhy.wcwt.helpers.WcwtToolkitStore", """
            package com.lhy.wcwt.helpers;
            import net.minecraft.world.entity.player.Player; import net.minecraft.world.item.ItemStack;
            public class WcwtToolkitStore {public static appeng.util.inv.AppEngInternalInventory items(Player player,ItemStack ignored){return player.state.items;}}
            """);
        source("FixtureRunner", """
            import java.lang.reflect.*; import java.util.*;
            import net.minecraft.world.entity.player.*; import net.minecraft.world.item.ItemStack; import net.minecraft.core.NonNullList;
            import com.lhy.wcwt.helpers.*; import com.lhy.wcwt.helpers.WcwtToolkitHotbarState.Bar;
            import net.bettercombat.logic.PlayerAttackHelper;
            public class FixtureRunner {
                static int passed;
                static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
                static ItemStack cell(Player p){return WcwtToolkitHotbarState.getSelectedToolkit(p);}
                static void cell(Player p,ItemStack s){int i=WcwtToolkitHotbarState.toolkitIndex(p);if(p.level.client)p.state.clientItems[i]=s;else p.state.items.setItemDirect(i,s);}
                static Player player(boolean client,Bar page){Player p=new Player();p.level.client=client;p.state.page=page;return p;}
                static ItemStack stack(String id){return new ItemStack(id,1);}
                static void run(boolean client,Bar page,boolean stackable,boolean brokenOffhand,boolean empty) {
                    Player p=player(client,page); ItemStack vanilla=stack("vanilla"),main=empty?ItemStack.EMPTY:stack("main"),off=new ItemStack("off",stackable?64:1);
                    p.inventory.items.set(4,vanilla);p.inventory.offhand.set(0,off);cell(p,main);
                    if(!empty)p.attributes.active.add("main");
                    PlayerAttackHelper.swapHandAttributes(p,true,()->{
                        check(p.getMainHandItem()==off,"callback must read actual offhand identity");
                        check(p.inventory.items.get(4)==vanilla,"temporary swap overwrote real hotbar");
                        check(p.getAttributes().active.equals(Set.of("off")),"offhand attributes during callback");
                        if(brokenOffhand)off.count=0;
                    });
                    check(cell(p)==main,"toolkit identity not restored");check(p.getOffhandItem()==off,"offhand identity not restored");
                    check(p.inventory.items.get(4)==vanilla,"tool copied into vanilla slot");
                    check(p.attributes.active.equals(empty?Set.of():Set.of("main")),"main attributes not restored");
                    check(!brokenOffhand||p.getOffhandItem().isEmpty(),"broken offhand resurrected");passed++;
                    if(!empty){PlayerAttackHelper.swapHandAttributes(p,false,()->{check(p.getMainHandItem()==main,"main attack wrong hand");main.count=0;});
                        check(cell(p).isEmpty()&&p.inventory.items.get(4)==vanilla,"broken toolkit tool resurrected or copied");passed++;}
                }
                static void fallback(boolean menu,boolean spectator,boolean center) {
                    Player p=player(false,center?Bar.CENTER:Bar.LEFT);p.spectator=spectator;
                    if(menu)p.containerMenu=new net.minecraft.world.inventory.AbstractContainerMenu();
                    ItemStack main=stack("vanilla"),off=stack("off");p.inventory.items.set(4,main);p.inventory.offhand.set(0,off);p.attributes.active.add("vanilla");
                    PlayerAttackHelper.swapHandAttributes(p,true,()->check(p.getMainHandItem()==off,"fallback swap did not use vanilla slot"));
                    check(p.inventory.items.get(4)==main&&p.getOffhandItem()==off,"fallback did not restore vanilla/offhand");passed++;
                }
                public static void main(String[] ignored) throws Exception {
                    for(boolean client:new boolean[]{false,true})for(Bar page:new Bar[]{Bar.LEFT,Bar.RIGHT}) {
                        run(client,page,false,false,false);run(client,page,false,true,false);
                        run(client,page,true,false,false);run(client,page,false,false,true);
                    }
                    fallback(true,false,false);fallback(false,true,false);fallback(false,false,true);
                    Player p=player(false,Bar.RIGHT);ItemStack tool=stack("tool");cell(p,tool);
                    Method handler=Arrays.stream(PlayerAttackHelper.class.getDeclaredMethods()).filter(m->m.getName().contains("wcwt$setSwappedHand")).findFirst().orElseThrow();handler.setAccessible(true);
                    NonNullList<ItemStack> unrelated=new NonNullList<>(36,ItemStack.EMPTY);ItemStack other=stack("other");
                    Object previous=handler.invoke(null,unrelated,4,other,p,true,(Runnable)()->{});
                    check(previous==ItemStack.EMPTY&&unrelated.get(4)==other&&cell(p)==tool,"non-player list redirected");passed++;
                    previous=handler.invoke(null,p.inventory.items,3,other,p,true,(Runnable)()->{});
                    check(previous==ItemStack.EMPTY&&p.inventory.items.get(3)==other&&cell(p)==tool,"non-selected index redirected");passed++;
                    previous=handler.invoke(null,p.inventory.items,4,other,p,true,(Runnable)()->{});
                    check(previous==tool&&cell(p)==other,"redirect did not preserve NonNullList.set return contract");passed++;
                    System.out.println("PASS: "+passed+" behavior scenarios using actual transformed BC swap/attribute methods and actual WCWT hand/cell method bytecode (game/AE fixtures)");
                }
            }
            """);
        List<String> compileArgs = new ArrayList<>(List.of("-proc:none","-cp",out+File.pathSeparator+System.getProperty("java.class.path"),"-d",out.toString()));
        try(var paths=Files.walk(root.resolve("src"))){paths.filter(p->p.toString().endsWith(".java")).forEach(p->compileArgs.add(p.toString()));}
        if(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,compileArgs.toArray(String[]::new))!=0)throw new AssertionError("fixture compilation failed");
        String guava=Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator)).filter(p->Path.of(p).getFileName().toString().startsWith("guava-")).findFirst().orElseThrow();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{out.toUri().toURL(),Path.of(guava).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
            loader.loadClass("FixtureRunner").getMethod("main",String[].class).invoke(null,(Object)new String[0]);
        }
    }
}
