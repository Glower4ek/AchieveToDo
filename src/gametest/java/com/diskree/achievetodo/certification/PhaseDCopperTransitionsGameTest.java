package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.ability.PhaseDAbilityMappingTest.CaseContract;
import com.diskree.achievetodo.ability.PhaseDAbilityMappingTest.Outcome;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.server.AchieveToDoServer;
import com.diskree.achievetodo.ability.*;
import com.diskree.achievetodo.injection.extension.main.MountInventoryMenuExtension;
import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.network.HashedStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.nautilus.Nautilus;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import java.lang.reflect.Field;
import java.util.*;

import java.util.List;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.nbt.NbtIo;
import java.nio.file.*;

public final class PhaseDCopperTransitionsGameTest {
    private static Block copperBlock(String id) {
        Block block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:"+id));
        if(block==null||block==Blocks.AIR)throw new IllegalArgumentException(id);return block;
    }
    private BlockPos put(int x,int y,int z,BlockState state) {
        BlockPos pos=helper.absolutePos(new BlockPos(x,y,z));blocks.putIfAbsent(pos,helper.getLevel().getBlockState(pos));
        helper.getLevel().setBlockAndUpdate(pos,state);return pos;
    }
    private void blockAt(BlockPos pos,BlockState state) {
        blocks.putIfAbsent(pos,helper.getLevel().getBlockState(pos));helper.getLevel().setBlockAndUpdate(pos,state);
    }
    private Set<String> criteria(Joined f) {
        Set<String> completed=new TreeSet<>();
        for(var advancement:helper.getLevel().getServer().getAdvancements().getAllAdvancements())
            for(String criterion:f.player().getAdvancements().getOrStartProgress(advancement).getCompletedCriteria())completed.add(advancement.id()+"#"+criterion);
        return completed;
    }
    private InteractionResult use(Joined f,BlockPos pos,InteractionHand hand) {
        var p=f.player();var before=helper.getLevel().getBlockState(pos);var stack=p.getItemInHand(hand).copy();var progress=criteria(f);
        InteractionResult result=p.gameMode.useItemOn(p,helper.getLevel(),p.getItemInHand(hand),hand,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        var after=new TreeSet<>(criteria(f));after.removeAll(progress);
        System.out.println("D_S05_BLOCK_STATE "+new Gson().toJson(Map.of("scenario",scenario,"player",p.getUUID(),"hand",hand,"pos",pos.toString(),"before",before.toString(),"after",helper.getLevel().getBlockState(pos).toString(),"item_before",stack.toString(),"item_after",p.getItemInHand(hand).toString(),"criterion_gains",after,"result",result.toString())));
        return result;
    }
    private void blockLandmark(Joined f,BlockPos protectedPos,boolean locked) throws Exception {
        boxes().clear();membership().clear();var box=new BoundingBox(protectedPos);
        boxes().put(new ChunkPos(protectedPos.getX() >> 4,protectedPos.getZ() >> 4),new HashMap<>(Map.of(LandmarkType.DESERT_PYRAMID,new HashSet<>(Set.of(new DimensionalBlockBox(DimensionType.OVERWORLD,box))))));
        membership().put(LandmarkType.DESERT_PYRAMID,new HashSet<>(Set.of(f.player().getUUID())));
        score(f,locked?threshold(AbilityType.findByLandmarkType(LandmarkType.DESERT_PYRAMID))-1:10000);observe(f);
        require("exact protected block permission",owner().isTargetInLockedLandmark(f.player(),DimensionType.OVERWORLD,new AABB(protectedPos),true)==locked,protectedPos);
    }
    private List<Object> observe(Joined actor) {
        List<Object> packets=new ArrayList<>(drain(actor));
        for(Joined other:joined)if(other!=actor)packets.addAll(drain(other));
        return packets;
    }
    private long effects(List<Object> packets) {
        return packets.stream().filter(p -> p instanceof ClientboundSoundPacket||p instanceof ClientboundSoundEntityPacket||p instanceof ClientboundLevelEventPacket).count();
    }
    private long notifications(List<Object> packets) {
        return packets.stream().filter(p -> p instanceof ClientboundSystemChatPacket chat && chat.overlay()).count();
    }
    private void secondary(Joined f) throws Exception {
        score(f,10000);f.player().setShiftKeyDown(true);require("secondary use reaches item route",f.player().isSecondaryUseActive(),true);observe(f);
    }
    private static void weatherTick(CopperGolem golem,long value) throws Exception {
        Field field=CopperGolem.class.getDeclaredField("nextWeatheringTick");field.setAccessible(true);field.setLong(golem,value);
    }
    private static long weatherTick(CopperGolem golem) throws Exception {
        return (Long)field(golem,CopperGolem.class,"nextWeatheringTick");
    }
    private void noEffects(Joined f,Set<String> progress,String action) {
        var packets=observe(f);require(action+" no native sound/level event",effects(packets)==0,effects(packets));
        require(action+" fallback does not duplicate permission feedback",notifications(packets)<=1,notifications(packets));
        require(action+" no live criterion mutation",criteria(f).equals(progress),criteria(f).size());
    }

    @GameTest(maxTicks=200) public void axeCandidatesAndMaterials(GameTestHelper helper) {
        run(helper,"axeCandidatesAndMaterials",() -> {
            Joined f=join();secondary(f);
            Item[] axes={Items.WOODEN_AXE,Items.STONE_AXE,Items.COPPER_AXE,Items.IRON_AXE,Items.GOLDEN_AXE,Items.DIAMOND_AXE,Items.NETHERITE_AXE};
            for(Item axe:axes) for(String action:List.of("strip","scrape","wax_off")) for(InteractionHand hand:InteractionHand.values()) {
                BlockState before=switch(action){case "strip" -> Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X);case "scrape" -> copperBlock("exposed_chiseled_copper").defaultBlockState();default -> copperBlock("waxed_chiseled_copper").defaultBlockState();};
                Block expected=switch(action){case "strip" -> Blocks.STRIPPED_OAK_LOG;default -> copperBlock("chiseled_copper");};
                BlockPos pos=put(3,2,3,before);hands(f.player(),hand,axe);score(f,threshold(AbilityType.findToolMaterialUsageAbility(axe))-1);observe(f);var progress=criteria(f);
                var result=use(f,pos,hand);require("locked actual material is nonconsuming",!result.consumesAction(),axe+" "+action+" "+hand+" "+result);
                require("locked candidate state and wear conserved",helper.getLevel().getBlockState(pos).equals(before)&&f.player().getItemInHand(hand).getDamageValue()==0,before);noEffects(f,progress,"locked "+action);
                score(f,10000);observe(f);require("allowed candidate native consumes",use(f,pos,hand).consumesAction(),axe+" "+action);
                if(axe==Items.WOODEN_AXE&&action.equals("strip")&&hand==InteractionHand.MAIN_HAND)
                    require("real BACAP strip criterion false to true through native use",!progress.contains("blazeandcave:building/stripper#stripped_oak_log")&&criteria(f).contains("blazeandcave:building/stripper#stripped_oak_log"),"ordinary ITEM_USED_ON_BLOCK; no direct award");
                require("allowed exact transition",helper.getLevel().getBlockState(pos).is(expected),helper.getLevel().getBlockState(pos));
                require("allowed axe wear one",f.player().getItemInHand(hand).getDamageValue()==1,f.player().getItemInHand(hand));
                if(action.equals("strip"))require("strip preserves axis",helper.getLevel().getBlockState(pos).getValue(RotatedPillarBlock.AXIS)==Direction.Axis.X,"X");
                require("allowed native sound/event observed",effects(observe(f))>0,action);
            }
            hands(f.player(),InteractionHand.MAIN_HAND,Items.COPPER_AXE);score(f,0);observe(f);BlockPos stone=put(3,2,3,Blocks.STONE.defaultBlockState());var progress=criteria(f);
            require("no candidate PASS",use(f,stone,InteractionHand.MAIN_HAND)==InteractionResult.PASS,"stone");var packets=observe(f);
            require("no candidate no permission notification/effects/wear",feedback(packets)==0&&notifications(packets)==0&&effects(packets)==0&&f.player().getMainHandItem().getDamageValue()==0,packets.size());require("no candidate criterion unchanged",criteria(f).equals(progress),progress.size());
        });
    }

    @GameTest(maxTicks=200) public void golemManualBranches(GameTestHelper helper) {
        run(helper,"golemManualBranches",() -> {
            Joined f=join();CopperGolem golem=spawn(EntityTypes.COPPER_GOLEM,f);golem.setCustomName(Component.literal("D-S05 golem"));
            for(InteractionHand hand:InteractionHand.values()) {
                hands(f.player(),hand,Items.AIR);golem.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND,3));landmark(f,golem,true);var progress=criteria(f);int drops=drops().size();
                require("retrieval outer Mob normalizes denial PASS",interact(f,golem,hand)==InteractionResult.PASS,"retrieve");
                require("retrieval denied held/drop conserved",golem.getMainHandItem().getCount()==3&&drops().size()==drops,"retrieve");noEffects(f,progress,"retrieval denied");
                landmark(f,golem,false);require("retrieval allowed native success",interact(f,golem,hand).consumesAction(),hand);
                require("retrieval throws exactly carried stack",golem.getMainHandItem().isEmpty()&&drops().stream().mapToInt(d -> d.getItem().is(Items.DIAMOND)?d.getItem().getCount():0).sum()==3,"3 diamonds");drops().forEach(Entity::discard);observe(f);
                golem.setItemSlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA,new ItemStack(Items.POPPY));hands(f.player(),hand,Items.SHEARS);boxes().clear();membership().clear();score(f,threshold(AbilityType.USE_SHEARS)-1);observe(f);progress=criteria(f);
                require("flower native eligible",golem.readyForShearing(),golem.getItemBySlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA));
                require("flower denied outer PASS",interact(f,golem,hand)==InteractionResult.PASS,hand);
                require("denied flower and shears conserved",golem.readyForShearing()&&f.player().getItemInHand(hand).getDamageValue()==0,hand);noEffects(f,progress,"flower denied");
                score(f,10000);observe(f);require("flower allowed",interact(f,golem,hand).consumesAction()&&!golem.readyForShearing(),hand);
                require("flower shears wear one",f.player().getItemInHand(hand).getDamageValue()==1,hand);drops().forEach(Entity::discard);observe(f);
                for(String action:List.of("wax","wax_off","scrape")) {
                    golem.setWeatherState(action.equals("scrape")?WeatheringCopper.WeatherState.WEATHERED:WeatheringCopper.WeatherState.UNAFFECTED);weatherTick(golem,action.equals("wax_off")?-2:-1);
                    hands(f.player(),hand,action.equals("wax")?Items.HONEYCOMB:Items.COPPER_AXE);landmark(f,golem,true);progress=criteria(f);long tick=weatherTick(golem);var age=golem.getWeatherState();
                    require("golem eligible landmark denial outer PASS",interact(f,golem,hand)==InteractionResult.PASS,action);
                    require("golem denial complete state/item conservation",weatherTick(golem)==tick&&golem.getWeatherState()==age&&f.player().getItemInHand(hand).getCount()==1&&f.player().getItemInHand(hand).getDamageValue()==0,action);noEffects(f,progress,action+" landmark denial");
                    boxes().clear();membership().clear();score(f,threshold(AbilityType.USE_COPPER_TOOLS)-1);observe(f);
                    if(!action.equals("wax")) {progress=criteria(f);require("golem actual copper material denial",interact(f,golem,hand)==InteractionResult.PASS&&weatherTick(golem)==tick&&golem.getWeatherState()==age,action);noEffects(f,progress,action+" material denial");score(f,10000);}
                    require("golem native eligible allowed",interact(f,golem,hand).consumesAction(),action);
                    require("golem exact allowed transition",action.equals("wax")?weatherTick(golem)==-2:action.equals("wax_off")?weatherTick(golem)==-1:golem.getWeatherState()==WeatheringCopper.WeatherState.EXPOSED,action);
                    require("golem exact consumption or wear",action.equals("wax")?f.player().getItemInHand(hand).isEmpty():f.player().getItemInHand(hand).getDamageValue()==1,action);observe(f);
                }
                hands(f.player(),hand,Items.COPPER_AXE);golem.setWeatherState(WeatheringCopper.WeatherState.UNAFFECTED);weatherTick(golem,-1);score(f,0);observe(f);progress=criteria(f);
                require("golem invalid axe native PASS",interact(f,golem,hand)==InteractionResult.PASS,hand);var packets=observe(f);require("golem no-op no feedback or effects",feedback(packets)==0&&notifications(packets)==0&&effects(packets)==0&&criteria(f).equals(progress),hand);
                hands(f.player(),hand,Items.HONEYCOMB);weatherTick(golem,-2);observe(f);require("already waxed no-op PASS",interact(f,golem,hand)==InteractionResult.PASS&&f.player().getItemInHand(hand).getCount()==1,hand);require("already waxed no feedback",feedback(observe(f))==0,hand);
            }
            score(f,0);golem.setWeatherState(WeatheringCopper.WeatherState.EXPOSED);weatherTick(golem,helper.getLevel().getGameTime()-1);
            var update=CopperGolem.class.getDeclaredMethod("updateWeathering",net.minecraft.server.level.ServerLevel.class,net.minecraft.util.RandomSource.class,long.class);update.setAccessible(true);
            update.invoke(golem,helper.getLevel(),net.minecraft.util.RandomSource.create(5),helper.getLevel().getGameTime());
            require("autonomous weathering unaffected by locked player",golem.getWeatherState()==WeatheringCopper.WeatherState.WEATHERED,golem.getWeatherState());observe(f);
        });
    }

    @GameTest(maxTicks=200) public void statueOwnersPoseAndReanimation(GameTestHelper helper) {
        run(helper,"statueOwnersPoseAndReanimation",() -> {
            Joined f=join();ServerPlayer p=f.player();
            for(String id:List.of("copper_golem_statue","exposed_copper_golem_statue","weathered_copper_golem_statue","oxidized_copper_golem_statue","waxed_copper_golem_statue","waxed_exposed_copper_golem_statue","waxed_weathered_copper_golem_statue","waxed_oxidized_copper_golem_statue")) {
                BlockState before=copperBlock(id).defaultBlockState().setValue(CopperGolemStatueBlock.FACING,Direction.WEST);BlockPos pos=put(3,2,3,before);
                var be=(CopperGolemStatueBlockEntity)helper.getLevel().getBlockEntity(pos);be.setComponents(DataComponentMap.builder().set(DataComponents.CUSTOM_NAME,Component.literal("D-S05 statue")).build());var components=be.components();
                p.setShiftKeyDown(false);hands(p,InteractionHand.MAIN_HAND,Items.STICK);blockLandmark(f,pos,true);var progress=criteria(f);
                require("statue pose denied FAIL",use(f,pos,InteractionHand.MAIN_HAND)==InteractionResult.PASS,"native fallback PASS "+id);
                require("statue pose denial state/BE/components intact",helper.getLevel().getBlockState(pos).equals(before)&&helper.getLevel().getBlockEntity(pos)==be&&be.components().equals(components),id);noEffects(f,progress,"pose denied "+id);
                boxes().clear();membership().clear();score(f,0);observe(f);
                require("pose has no material gate",use(f,pos,InteractionHand.MAIN_HAND).consumesAction(),id);
                require("pose changes exact next and preserves orientation/BE",helper.getLevel().getBlockState(pos).getValue(CopperGolemStatueBlock.POSE)==before.getValue(CopperGolemStatueBlock.POSE).getNextPose()&&helper.getLevel().getBlockState(pos).getValue(CopperGolemStatueBlock.FACING)==Direction.WEST&&helper.getLevel().getBlockEntity(pos)==be&&be.components().equals(components),id);observe(f);
                var serialized=be.saveWithFullMetadata(helper.getLevel().registryAccess());Path file=Path.of("d_s05_statue_"+id+".nbt");NbtIo.write(serialized,file);var restored=BlockEntity.loadStatic(pos,helper.getLevel().getBlockState(pos),NbtIo.read(file),helper.getLevel().registryAccess());
                require("statue actual disk bytes native decode supported name",restored instanceof CopperGolemStatueBlockEntity&&restored.components().equals(components),id);
                System.out.println("D_S05_PERSISTENCE "+new Gson().toJson(Map.of("file",file.toString(),"bytes",Files.size(file),"state",helper.getLevel().getBlockState(pos).toString(),"payload",serialized.toString(),"scope","Actual NBT write/read native decode; chunk/server restart NOT_RUN")));
                if(id.startsWith("waxed_")) {
                    hands(p,InteractionHand.MAIN_HAND,Items.HONEYCOMB);var pose=helper.getLevel().getBlockState(pos).getValue(CopperGolemStatueBlock.POSE);
                    require("waxed honeycomb takes earlier pose route without consumption",use(f,pos,InteractionHand.MAIN_HAND).consumesAction()&&p.getMainHandItem().getCount()==1&&helper.getLevel().getBlockState(pos).getValue(CopperGolemStatueBlock.POSE)==pose.getNextPose(),id);observe(f);
                }
                boolean waxing=id.equals("copper_golem_statue");hands(p,InteractionHand.MAIN_HAND,waxing?Items.HONEYCOMB:Items.COPPER_AXE);
                BlockState transitionBefore=helper.getLevel().getBlockState(pos);
                if(!waxing) {
                    score(f,threshold(AbilityType.USE_COPPER_TOOLS)-1);observe(f);progress=criteria(f);
                    require("statue item transition material veto preserves BE",use(f,pos,InteractionHand.MAIN_HAND)==InteractionResult.PASS&&helper.getLevel().getBlockState(pos).equals(transitionBefore)&&helper.getLevel().getBlockEntity(pos)==be&&p.getMainHandItem().getDamageValue()==0,id);noEffects(f,progress,"statue item material denial "+id);score(f,10000);
                }
                String expectedTransition=id.startsWith("waxed_")?id.substring(6):switch(id) {
                    case "copper_golem_statue" -> "waxed_copper_golem_statue";
                    case "exposed_copper_golem_statue" -> "copper_golem_statue";
                    case "weathered_copper_golem_statue" -> "exposed_copper_golem_statue";
                    default -> "weathered_copper_golem_statue";
                };
                observe(f);require("statue native wax/scrape map transition",use(f,pos,InteractionHand.MAIN_HAND).consumesAction()&&helper.getLevel().getBlockState(pos).is(copperBlock(expectedTransition)),id);
                require("statue wax/scrape retains same BE supported name pose rotation",helper.getLevel().getBlockEntity(pos)==be&&be.components().equals(components)&&helper.getLevel().getBlockState(pos).getValue(CopperGolemStatueBlock.POSE)==transitionBefore.getValue(CopperGolemStatueBlock.POSE)&&helper.getLevel().getBlockState(pos).getValue(CopperGolemStatueBlock.FACING)==Direction.WEST,id);observe(f);
                blockAt(pos,before);
                helper.getLevel().removeBlockEntity(pos);hands(p,InteractionHand.MAIN_HAND,Items.STICK);blockLandmark(f,pos,true);observe(f);progress=criteria(f);BlockState noBe=helper.getLevel().getBlockState(pos);var missing=use(f,pos,InteractionHand.MAIN_HAND);
                require("missing BE route nonconsuming",missing==InteractionResult.PASS,id+" "+missing);
                var recovered=helper.getLevel().getChunkAt(pos).getBlockEntity(pos);
                require("missing BE native owner-specific lifecycle",id.startsWith("waxed_")?recovered==null:recovered instanceof CopperGolemStatueBlockEntity&&recovered!=be,id+" "+recovered);
                require("missing BE no pose mutation",helper.getLevel().getBlockState(pos).equals(noBe),id);noEffects(f,progress,"missing BE "+id);
                if(!id.startsWith("waxed_")) {
                    var stale=(CopperGolemStatueBlockEntity)recovered;stale.setRemoved();hands(p,InteractionHand.MAIN_HAND,Items.COPPER_AXE);observe(f);progress=criteria(f);
                    require("removed BE yields native null PASS without reanimation",use(f,pos,InteractionHand.MAIN_HAND)==InteractionResult.PASS&&helper.getLevel().getChunkAt(pos).getBlockEntity(pos)==null&&p.getMainHandItem().getDamageValue()==0,id);noEffects(f,progress,"native null BE "+id);
                }
                blockAt(pos,Blocks.AIR.defaultBlockState());
            }
            for(InteractionHand hand:InteractionHand.values()) {
                BlockState before=copperBlock("copper_golem_statue").defaultBlockState().setValue(CopperGolemStatueBlock.FACING,Direction.EAST);BlockPos pos=put(3,2,3,before);var be=(CopperGolemStatueBlockEntity)helper.getLevel().getBlockEntity(pos);
                be.setComponents(DataComponentMap.builder().set(DataComponents.CUSTOM_NAME,Component.literal("D-S05 reborn")).build());hands(p,hand,Items.COPPER_AXE);boxes().clear();membership().clear();score(f,threshold(AbilityType.USE_COPPER_TOOLS)-1);observe(f);var progress=criteria(f);
                require("reanimation material denied then native axe no-op",use(f,pos,hand)==InteractionResult.PASS,"denied");
                require("denied reanimation no removal/spawn/wear",helper.getLevel().getBlockEntity(pos)==be&&helper.getLevel().getBlockState(pos).equals(before)&&p.getItemInHand(hand).getDamageValue()==0&&helper.getLevel().getEntitiesOfClass(CopperGolem.class,region()).isEmpty(),hand);noEffects(f,progress,"reanimation material denied");
                blockLandmark(f,pos,true);progress=criteria(f);require("reanimation landmark denied",use(f,pos,hand)==InteractionResult.PASS&&helper.getLevel().getBlockEntity(pos)==be,hand);noEffects(f,progress,"reanimation landmark denied");
                blockLandmark(f,pos,false);observe(f);require("reanimation allowed native success",use(f,pos,hand).consumesAction(),hand);
                var spawned=helper.getLevel().getEntitiesOfClass(CopperGolem.class,region());entities.addAll(spawned);
                require("reanimation one fresh named oriented entity",spawned.size()==1&&spawned.getFirst().getCustomName().getString().equals("D-S05 reborn")&&spawned.getFirst().getYRot()==Direction.EAST.toYRot(),spawned.size());
                require("reanimation native statue removal and axe wear",helper.getLevel().getBlockState(pos).isAir()&&helper.getLevel().getBlockEntity(pos)==null&&p.getItemInHand(hand).getDamageValue()==1,hand);spawned.forEach(Entity::discard);observe(f);
            }
        });
    }

    @GameTest(maxTicks=200) public void honeycombLinkedBlocksAndPersistence(GameTestHelper helper) {
        run(helper,"honeycombLinkedBlocksAndPersistence",() -> {
            Joined f=join();secondary(f);
            for(InteractionHand hand:InteractionHand.values()) {
                BlockPos pos=put(3,2,3,copperBlock("chiseled_copper").defaultBlockState());hands(f.player(),hand,Items.HONEYCOMB);blockLandmark(f,pos,true);var progress=criteria(f);
                require("wax landmark denies FAIL",use(f,pos,hand)==InteractionResult.FAIL,hand);
                require("wax denied item and block intact",f.player().getItemInHand(hand).getCount()==1&&helper.getLevel().getBlockState(pos).is(copperBlock("chiseled_copper")),hand);noEffects(f,progress,"wax denied");
                boxes().clear();membership().clear();score(f,0);observe(f);require("wax allowed with all material permissions locked",use(f,pos,hand).consumesAction()&&helper.getLevel().getBlockState(pos).is(copperBlock("waxed_chiseled_copper")),hand);
                require("wax consumes exactly one",f.player().getItemInHand(hand).isEmpty(),hand);observe(f);
                hands(f.player(),hand,Items.HONEYCOMB);blockLandmark(f,pos,true);progress=criteria(f);require("already waxed PASS",use(f,pos,hand)==InteractionResult.PASS,hand);var packets=observe(f);require("no transition no feedback/consumption/effects",feedback(packets)==0&&notifications(packets)==0&&effects(packets)==0&&f.player().getItemInHand(hand).getCount()==1&&criteria(f).equals(progress),hand);
            }
            for(boolean axe:List.of(false,true)) {
                String door=axe?"waxed_copper_door":"copper_door";BlockState lower=copperBlock(door).defaultBlockState().setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER).setValue(DoorBlock.FACING,Direction.WEST).setValue(DoorBlock.OPEN,true);
                BlockPos bottom=put(3,1,4,Blocks.STONE.defaultBlockState()).above();blockAt(bottom,lower);BlockPos top=bottom.above();blockAt(top,lower.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
                hands(f.player(),InteractionHand.MAIN_HAND,axe?Items.WOODEN_AXE:Items.HONEYCOMB);blockLandmark(f,top,true);var before=helper.getLevel().getBlockState(bottom);var upper=helper.getLevel().getBlockState(top);var progress=criteria(f);
                require("only partner locked clicked block clear",!owner().isTargetInLockedLandmark(f.player(),DimensionType.OVERWORLD,new AABB(bottom),true),bottom);
                require("linked door denied nonconsuming",!use(f,bottom,InteractionHand.MAIN_HAND).consumesAction(),axe);
                require("linked door denial atomic",helper.getLevel().getBlockState(bottom).equals(before)&&helper.getLevel().getBlockState(top).equals(upper)&&f.player().getMainHandItem().getCount()==1&&f.player().getMainHandItem().getDamageValue()==0,axe);noEffects(f,progress,"door partner denied");
                blockLandmark(f,top,false);observe(f);require("linked door allowed",use(f,bottom,InteractionHand.MAIN_HAND).consumesAction(),axe);
                Block expected=copperBlock(axe?"copper_door":"waxed_copper_door");require("both door halves native synchronized properties",helper.getLevel().getBlockState(top).is(expected)&&helper.getLevel().getBlockState(bottom).is(expected)&&helper.getLevel().getBlockState(bottom).getValue(DoorBlock.OPEN)&&helper.getLevel().getBlockState(top).getValue(DoorBlock.FACING)==Direction.WEST,axe);observe(f);
            }
            for(boolean axe:List.of(false,true)) {
                Block chest=copperBlock(axe?"waxed_copper_chest":"copper_chest");BlockState left=chest.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH).setValue(ChestBlock.TYPE,ChestType.LEFT);
                BlockPos a=put(5,2,4,left);BlockPos b=ChestBlock.getConnectedBlockPos(a,left);blockAt(b,left.setValue(ChestBlock.TYPE,ChestType.RIGHT));
                var first=(ChestBlockEntity)helper.getLevel().getBlockEntity(a);var second=(ChestBlockEntity)helper.getLevel().getBlockEntity(b);ItemStack namedChest=new ItemStack(chest.asItem());namedChest.set(DataComponents.CUSTOM_NAME,Component.literal("D-S05 chest"));first.applyComponentsFromItemStack(namedChest);first.setItem(0,new ItemStack(Items.DIAMOND,7));second.setItem(26,new ItemStack(Items.EMERALD,9));var saved=first.saveWithFullMetadata(helper.getLevel().registryAccess());
                hands(f.player(),InteractionHand.MAIN_HAND,axe?Items.WOODEN_AXE:Items.HONEYCOMB);blockLandmark(f,b,true);var progress=criteria(f);
                require("chest pair partner locked veto",!use(f,a,InteractionHand.MAIN_HAND).consumesAction()&&helper.getLevel().getBlockState(a).is(chest)&&helper.getLevel().getBlockState(b).is(chest),axe);noEffects(f,progress,"chest partner denied");
                blockLandmark(f,b,false);observe(f);require("chest pair transition allowed",use(f,a,InteractionHand.MAIN_HAND).consumesAction(),axe);
                require("chest BE retained both identities inventories and name",helper.getLevel().getBlockEntity(a)==first&&helper.getLevel().getBlockEntity(b)==second&&first.getItem(0).getCount()==7&&second.getItem(26).getCount()==9&&first.getCustomName().getString().equals("D-S05 chest"),axe);
                Block expected=copperBlock(axe?"copper_chest":"waxed_copper_chest");require("chest pair native synchronization",helper.getLevel().getBlockState(a).is(expected)&&helper.getLevel().getBlockState(b).is(expected),axe);
                var serialized=first.saveWithFullMetadata(helper.getLevel().registryAccess());Path file=Path.of("d_s05_chest_"+axe+".nbt");NbtIo.write(serialized,file);var read=NbtIo.read(file);require("real persisted NBT bytes readable",read!=null&&read.equals(serialized),Files.size(file));
                var restored=BlockEntity.loadStatic(a,helper.getLevel().getBlockState(a),read,helper.getLevel().registryAccess());
                require("independent native BE decode inventory/name",restored instanceof ChestBlockEntity copy&&copy.getItem(0).getCount()==7&&copy.getCustomName().getString().equals("D-S05 chest"),restored);
                System.out.println("D_S05_PERSISTENCE "+new Gson().toJson(Map.of("file",file.toString(),"bytes",Files.size(file),"before",saved.toString(),"after",serialized.toString(),"scope","Actual NBT file write/read + native BlockEntity.loadStatic; full chunk/server restart NOT_RUN")));observe(f);
                first.clearContent();second.clearContent();
            }
            // Opening has the historical SUCCESS veto; observe the actual outer result without refining CP-CHEST.
            BlockPos dispenser=put(7,2,3,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST));
            BlockPos dispensedTarget=dispenser.east();blockAt(dispensedTarget,copperBlock("chiseled_copper").defaultBlockState());
            var inventory=(DispenserBlockEntity)helper.getLevel().getBlockEntity(dispenser);inventory.setItem(0,new ItemStack(Items.HONEYCOMB,3));blockLandmark(f,dispensedTarget,true);score(f,0);
            var dispense=DispenserBlock.class.getDeclaredMethod("dispenseFrom",net.minecraft.server.level.ServerLevel.class,BlockState.class,BlockPos.class);dispense.setAccessible(true);
            dispense.invoke(Blocks.DISPENSER,helper.getLevel(),helper.getLevel().getBlockState(dispenser),dispenser);
            require("actual dispenser wax bypasses manual player gate",helper.getLevel().getBlockState(dispensedTarget).is(copperBlock("waxed_chiseled_copper"))&&inventory.getItem(0).getCount()==2,inventory.getItem(0));inventory.clearContent();observe(f);
            BlockPos oxidizing=put(8,2,5,copperBlock("chiseled_copper").defaultBlockState());blockLandmark(f,oxidizing,true);score(f,0);
            Block natural=copperBlock("chiseled_copper");var randomTick=natural.getClass().getDeclaredMethod("randomTick",BlockState.class,net.minecraft.server.level.ServerLevel.class,BlockPos.class,net.minecraft.util.RandomSource.class);randomTick.setAccessible(true);
            var rng=net.minecraft.util.RandomSource.create(17);
            for(int i=0;i<512&&helper.getLevel().getBlockState(oxidizing).is(natural);i++)randomTick.invoke(natural,helper.getLevel().getBlockState(oxidizing),helper.getLevel(),oxidizing,rng);
            require("native natural oxidation algorithm has no player gate",helper.getLevel().getBlockState(oxidizing).is(copperBlock("exposed_chiseled_copper")),helper.getLevel().getBlockState(oxidizing));observe(f);
            BlockPos source=put(8,2,7,copperBlock("copper_chest").defaultBlockState());BlockPos destination=put(8,2,9,Blocks.CHEST.defaultBlockState());
            var sourceInventory=(ChestBlockEntity)helper.getLevel().getBlockEntity(source);var destinationInventory=(ChestBlockEntity)helper.getLevel().getBlockEntity(destination);sourceInventory.setItem(0,new ItemStack(Items.DIAMOND,24));destinationInventory.setItem(0,new ItemStack(Items.DIAMOND,2));
            blockLandmark(f,source,true);score(f,0);CopperGolem carrier=spawn(EntityTypes.COPPER_GOLEM,f);
            Class<?> transport=Class.forName("net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers");var pickup=transport.getDeclaredMethod("pickupItemFromContainer",net.minecraft.world.Container.class);pickup.setAccessible(true);
            ItemStack carried=(ItemStack)pickup.invoke(null,sourceInventory);carrier.setItemInHand(InteractionHand.MAIN_HAND,carried);
            require("native autonomous pickup cap16 no player permission",carried.getCount()==16&&sourceInventory.getItem(0).getCount()==8,carried);
            var deposit=transport.getDeclaredMethod("addItemsToContainer",PathfinderMob.class,net.minecraft.world.Container.class);deposit.setAccessible(true);ItemStack remaining=(ItemStack)deposit.invoke(null,carrier,destinationInventory);carrier.setItemInHand(InteractionHand.MAIN_HAND,remaining);
            require("native autonomous deposit inventory conservation",remaining.isEmpty()&&destinationInventory.getItem(0).getCount()==18&&sourceInventory.getItem(0).getCount()==8,remaining);sourceInventory.clearContent();destinationInventory.clearContent();observe(f);
            BlockPos chest=put(5,2,4,copperBlock("copper_chest").defaultBlockState());f.player().setShiftKeyDown(false);hands(f.player(),InteractionHand.MAIN_HAND,Items.AIR);score(f,0);boxes().clear();membership().clear();observe(f);var menu=f.player().containerMenu;
            var result=use(f,chest,InteractionHand.MAIN_HAND);require("historical chest denial SUCCESS menu unchanged",result==InteractionResult.SUCCESS&&f.player().containerMenu==menu,result);observe(f);
        });
    }
    private record Joined(ServerPlayer player, Connection connection, EmbeddedChannel channel) {}
    @FunctionalInterface private interface Scenario { void run() throws Exception; }
    private GameTestHelper helper;
    private String scenario;
    private int assertions;
    private final List<Joined> joined = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private final Map<BlockPos,BlockState> blocks = new HashMap<>();
    private final Map<DisplaySlot,Objective> displays = new EnumMap<>(DisplaySlot.class);
    private Objective objective;
    private boolean createdObjective;
    private Map<ChunkPos,Map<LandmarkType,Set<DimensionalBlockBox>>> priorBoxes;
    private Map<LandmarkType,Set<UUID>> priorMembership;
    private Set<UUID> priorItems;
    private AchieveToDoServer owner() { return AchieveToDoMod.getServer(); }
    private static Object field(Object value, Class<?> type, String name) throws Exception {
        Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(value);
    }
    private void require(String name, boolean passed, Object observed) {
        System.out.println("D_S05_ASSERT "+new Gson().toJson(Map.of("scenario",scenario,"assertion",name,
            "passed",passed,"observed",String.valueOf(observed),"seq",++assertions)));
        if(!passed)throw new AssertionError(name+": "+observed);
    }
    @SuppressWarnings("unchecked") private int threshold(AbilityType ability) throws Exception {
        int value=((Map<AbilityType,Integer>)field(owner(),AchieveToDoServer.class,"abilitiesConfiguration")).get(ability);
        require("live threshold positive "+ability,value>0,value);return value;
    }
    @SuppressWarnings("unchecked") private void score(Joined fixture,int count) throws Exception {
        helper.getLevel().getServer().getScoreboard().getOrCreatePlayerScore(fixture.player(),objective).set(count);drain(fixture);
        int observed=((Map<UUID,Integer>)field(owner(),AchieveToDoServer.class,"obtainedAdvancementsCountByPlayers")).getOrDefault(fixture.player().getUUID(),Integer.MIN_VALUE);
        require("live score reconciled",observed==count,observed);
    }
    private Joined join() {
        var server=helper.getLevel().getServer();UUID id=UUID.randomUUID();GameProfile profile=new GameProfile(id,"d5"+id.toString().substring(0,10));
        ServerPlayer player=new ServerPlayer(server,helper.getLevel(),profile,ClientInformation.createDefault());
        Connection connection=new Connection(PacketFlow.SERVERBOUND);EmbeddedChannel channel=new EmbeddedChannel(connection);
        Joined fixture=new Joined(player,connection,channel);joined.add(fixture);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection,player,CommonListenerCookie.createInitial(profile,false));
        player.setGameMode(GameType.SURVIVAL);new ServerboundPlayerLoadedPacket().handle(player.connection);
        BlockPos pos=helper.absolutePos(new BlockPos(1,2,1));blocks.putIfAbsent(pos.below(),helper.getLevel().getBlockState(pos.below()));
        helper.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        player.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);drain(fixture);
        require("joined loaded survival player",server.getPlayerList().getPlayer(id)==player&&player.connection.hasClientLoaded()&&!player.isCreative()&&!player.isSpectator(),id);
        return fixture;
    }
    private List<Object> drain(Joined fixture) {
        fixture.channel().runPendingTasks();fixture.channel().runScheduledPendingTasks();fixture.channel().flushOutbound();
        List<Object> packets=new ArrayList<>();Object packet;
        while((packet=fixture.channel().readOutbound())!=null) {
            if(packet instanceof ClientboundPlayerPositionPacket p)new ServerboundAcceptTeleportationPacket(p.id()).handle(fixture.player().connection);
            packets.add(packet);ReferenceCountUtil.release(packet);
            if(packets.size()>20000)throw new IllegalStateException("Unbounded fixture packet drain");
        }
        return packets;
    }
    private long feedback(List<Object> packets) {
        return packets.stream().filter(p -> p instanceof ClientboundCustomPayloadPacket c && c.payload() instanceof com.diskree.achievetodo.networking.s2c.CheckTargetInLockedLandmarkPayload).count();
    }
    private long opens(List<Object> packets) { return packets.stream().filter(p -> p instanceof ClientboundMountScreenOpenPacket).count(); }
    private long sounds(List<Object> packets) { return packets.stream().filter(p -> p instanceof ClientboundSoundPacket||p instanceof ClientboundSoundEntityPacket).count(); }
    private <T extends Mob> T spawn(EntityType<T> type,Joined fixture) {
        T target=helper.spawn(type,new BlockPos(1,2,3));entities.add(target);target.setNoAi(true);
        target.setPos(fixture.player().getX()+2,fixture.player().getY(),fixture.player().getZ());
        if(target instanceof AgeableMob ageable)ageable.setAge(0);return target;
    }
    @SuppressWarnings("unchecked") private Map<ChunkPos,Map<LandmarkType,Set<DimensionalBlockBox>>> boxes() throws Exception {
        return (Map<ChunkPos,Map<LandmarkType,Set<DimensionalBlockBox>>>)field(owner(),AchieveToDoServer.class,"landmarksByChunks");
    }
    @SuppressWarnings("unchecked") private Map<LandmarkType,Set<UUID>> membership() throws Exception {
        return (Map<LandmarkType,Set<UUID>>)field(owner(),AchieveToDoServer.class,"playersByLockedLandmarkTypes");
    }
    private void landmark(Joined fixture,Entity target,boolean locked) throws Exception {
        boxes().clear();membership().clear();
        BlockPos p=target.blockPosition();var box=new BoundingBox(p.getX()-1,p.getY()-1,p.getZ()-1,p.getX()+1,p.getY()+3,p.getZ()+1);
        var chunk=new HashMap<LandmarkType,Set<DimensionalBlockBox>>();
        chunk.put(LandmarkType.DESERT_PYRAMID,new HashSet<>(Set.of(new DimensionalBlockBox(DimensionType.OVERWORLD,box))));
        boxes().put(new ChunkPos(p.getX() >> 4,p.getZ() >> 4),chunk);
        membership().put(LandmarkType.DESERT_PYRAMID,new HashSet<>(Set.of(fixture.player().getUUID())));
        score(fixture,locked?threshold(AbilityType.findByLandmarkType(LandmarkType.DESERT_PYRAMID))-1:10000);
        require("actual target permission "+locked,owner().isTargetInLockedLandmark(fixture.player(),DimensionType.OVERWORLD,target.getBoundingBox(),true)==locked,target.getUUID());drain(fixture);
    }
    private AABB region() { return new AABB(Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(-10,-5,-10))),Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(25,15,25)))); }
    private List<ItemEntity> drops() { return helper.getLevel().getEntitiesOfClass(ItemEntity.class,region()).stream().filter(e -> !priorItems.contains(e.getUUID())).toList(); }
    private void hands(ServerPlayer player,InteractionHand hand,Item item) {
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        player.setItemInHand(hand,new ItemStack(item));
    }
    private InteractionResult interact(Joined fixture,Entity target,InteractionHand hand) {
        ServerPlayer player=fixture.player();String before=state(player,target,hand);
        InteractionResult result=player.interactOn(target,hand,Vec3.ZERO);
        System.out.println("D_S05_STATE "+new Gson().toJson(Map.of("scenario",scenario,"native_path","Player.interactOn/Entity.interact/subtype or ItemStack fallback",
            "player",player.getUUID(),"target",target.getUUID(),"hand",hand,"before",before,"after",state(player,target,hand),"result",result.toString())));
        return result;
    }
    private String state(ServerPlayer player,Entity target,InteractionHand hand) {
        return "secondary="+player.isSecondaryUseActive()+" baby="+(target instanceof LivingEntity living&&living.isBaby())+
            " health="+(target instanceof LivingEntity living?living.getHealth():0)+" tame="+
            (target instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse?horse.isTamed():target instanceof TamableAnimal tame?tame.isTame():"not_applicable")+
            " hand="+player.getItemInHand(hand)+" menu="+player.containerMenu.getClass().getSimpleName()+":"+player.containerMenu.containerId+
            " riding="+(player.getVehicle()==target)+" body="+(target instanceof LivingEntity living?living.getItemBySlot(EquipmentSlot.BODY):"none")+
            " saddle="+(target instanceof LivingEntity living?living.getItemBySlot(EquipmentSlot.SADDLE):"none");
    }
    private void cleanup() throws Exception {
        Throwable failure=null;
        for(Joined fixture:new ArrayList<>(joined))try {
            fixture.player().closeContainer();fixture.player().stopRiding();fixture.player().stopUsingItem();fixture.player().getAdvancements().save();drain(fixture);
            fixture.connection().disconnect(Component.literal("D-S05 fixture complete"));fixture.channel().runPendingTasks();fixture.channel().finishAndReleaseAll();
            if(!(Boolean)field(fixture.connection(),Connection.class,"disconnectionHandled"))fixture.connection().handleDisconnection();
            var server=helper.getLevel().getServer();server.getConnection().getConnections().remove(fixture.connection());
            require("player connection channel removed",server.getPlayerList().getPlayer(fixture.player().getUUID())==null&&!fixture.channel().isOpen()&&!server.getConnection().getConnections().contains(fixture.connection()),fixture.player().getUUID());
            server.getScoreboard().resetAllPlayerScores(fixture.player());
            require("temporary score removed",server.getScoreboard().getPlayerScoreInfo(fixture.player(),objective)==null,fixture.player().getUUID());joined.remove(fixture);
        }catch(Throwable t){if(failure==null)failure=t;else failure.addSuppressed(t);}
        for(Entity entity:entities){entity.discard();require("fixture entity discarded",entity.isRemoved(),entity.getUUID());}entities.clear();
        for(ItemEntity drop:drops())drop.discard();require("fixture drops removed",drops().isEmpty(),drops().size());
        boxes().clear();boxes().putAll(priorBoxes);membership().clear();membership().putAll(priorMembership);
        require("landmark maps restored",boxes().equals(priorBoxes)&&membership().equals(priorMembership),scenario);
        blocks.forEach((p,b) -> helper.getLevel().setBlockAndUpdate(p,b));
        for(var e:blocks.entrySet())require("world block restored",helper.getLevel().getBlockState(e.getKey()).equals(e.getValue()),e.getKey());blocks.clear();
        var board=helper.getLevel().getServer().getScoreboard();displays.forEach(board::setDisplayObjective);if(createdObjective){board.removeObjective(objective);createdObjective=false;}
        owner().prepareScoreboard(board);
        for(var e:displays.entrySet())require("scoreboard display restored",board.getDisplayObjective(e.getKey())==e.getValue(),e.getKey());
        if(failure!=null)throw new IllegalStateException("D-S05 cleanup failed",failure);
    }
    private void run(GameTestHelper helper,String name,Scenario body) {
        this.helper=helper;scenario=name;var board=helper.getLevel().getServer().getScoreboard();
        for(DisplaySlot slot:DisplaySlot.values())displays.put(slot,board.getDisplayObjective(slot));
        objective=board.getObjective("bac_advancements");if(objective==null){createdObjective=true;objective=board.addObjective("bac_advancements",ObjectiveCriteria.DUMMY,Component.literal("D-S05"),ObjectiveCriteria.RenderType.INTEGER,false,null);}
        board.setDisplayObjective(DisplaySlot.SIDEBAR,objective);owner().prepareScoreboard(board);
        helper.runAfterDelay(5,() -> {
            try {
                priorBoxes=new HashMap<>(boxes());priorMembership=new HashMap<>(membership());priorItems=new HashSet<>();
                helper.getLevel().getEntitiesOfClass(ItemEntity.class,region()).forEach(e -> priorItems.add(e.getUUID()));
                require("server permission ready",!owner().isNotReady(),name);Joined observer=join();score(observer,10000);body.run();cleanup();
                System.out.println("D_S05_GAMEPLAY_RECEIPT "+new Gson().toJson(Map.of("scenario",scenario,"status","PASS","assertions",assertions,"cleanup",true,"scope","Joined server native interactions; client GUI and save/reload NOT_RUN")));helper.succeed();
            }catch(Throwable t){t.printStackTrace();try{cleanup();}catch(Throwable c){t.addSuppressed(c);c.printStackTrace();}
                System.out.println("D_S05_GAMEPLAY_RECEIPT "+new Gson().toJson(Map.of("scenario",scenario,"status","FAIL","error",t.toString())));helper.fail(name+": "+t);}
        });
    }
    private static CaseContract wax(String suffix, Outcome outcome, String actor, Map<String, String> observations) {
        return PhaseDAbilityRoutesGameTest.future("P3:G06-EDGE-000-" + suffix, "T02/PE-01", "CH-15", "D-S05", outcome,
            "P3:t02_future_regression_matrix.json#/cases/0; SHA256 4a76ca1957bc28b149476440757cda7ba53de878d4a74e421393f3195a714262",
            "HoneycombItem.useOn/getWaxed; packet UseOnContext; MAIN/OFF_HAND; secondary-use reaches item route",
            actor, "getWaxed present; native enabled/permission/cooldown valid; no blocking MAIN_HAND intent",
            "Target landmark only; honeycomb does not acquire material tool gate", observations,
            List.of("Supported properties/linked state/BE payload retained; observe save/chunk reload/restart separately"));
    }

    public static final List<CaseContract> FUTURE_CASES = List.of(
        wax("SURVIVAL-ALLOW", Outcome.NATIVE_ALLOWED_ATD_ALLOWED,
            "Joined SURVIVAL ServerPlayer; landmark unlocked; material may be locked",
            Map.of("block_after", "minecraft:waxed_chiseled_copper", "honeycomb_consumed", "1")),
        wax("SURVIVAL-DENY", Outcome.NATIVE_ALLOWED_ATD_DENIED,
            "Joined SURVIVAL ServerPlayer; target landmark locked",
            Map.of("block_after", "minecraft:chiseled_copper", "honeycomb_consumed", "0",
                "denied_sound_delta", "0", "denied_criterion_delta", "0", "denied_linked_state_delta", "0")),
        wax("CREATIVE-CONTROL", Outcome.AUTONOMOUS_OR_ADMIN,
            "Creative packet control; baseline creative exemption, not initiating-player automation gate",
            Map.of("block_after", "minecraft:waxed_chiseled_copper", "honeycomb_consumed", "0")));

    @GameTest(maxTicks = 20)
    public void honeycombDoesNotAcquireToolGate(GameTestHelper helper) {
        PhaseDAbilityRoutesGameTest.runControl(helper, PhaseDAbilityRoutesGameTest.control(
            "D-S00-G-COPPER-CONTROL", Map.of("honeycomb_material_gate", "absent",
                "future_case_execution", "NOT_RUN")), () -> Map.of(
            "honeycomb_material_gate", AbilityType.findToolMaterialUsageAbility(Items.HONEYCOMB) == null ? "absent" : "present",
            "future_case_execution", FUTURE_CASES.stream().allMatch(c -> c.initialExecutionStatus().equals("NOT_RUN")) ? "NOT_RUN" : "invalid"));
    }

}
