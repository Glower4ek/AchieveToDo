"""Review-only proposal. Never writes production paths or promotes diagnostic receipts."""
import difflib, hashlib, json, pathlib
from checkpoint import ROOT, BASE, sha

def main():
    scratch=ROOT/'build/tmp/final19_implementation'
    evidence=json.loads((scratch/'cauldron_gate_packet_diagnosis.json').read_text(encoding='utf-8'))
    assert evidence['diagnosticOnly'] and evidence['classification']=='PRODUCTION_BUG'
    assert {r['criterion'] for r in evidence['entries']}=={'clean_leather_armor','clean_banner','clean_shulker_box'}
    assert len({r['playerUuid'] for r in evidence['entries']})==len(evidence['entries'])==3
    for r in evidence['entries']:
        assert r['nativeBoundary']=='ServerboundUseItemOnPacket.handle' and r['gameMode']=='SURVIVAL'
        assert all(r[k] for k in ['joined','connectionRegistered','clientLoaded','finiteMaterials','lockedBefore','lockedAfter','wrongItemNegative','itemTagMember','blockTagMember'])
        assert r['scoreAfter']<r['liveThreshold'] and not r['criterionBefore'] and r['criterionAfter']
        assert r['blockBefore']==r['blockAfter'] and r['itemBefore']==r['itemAfter']
        assert all(r['cleanup'][k] for k in ['playerRemoved','connectionRemoved','channelSettled','blocksRestored'])
    assert (scratch/'cauldron_gate_packet_diagnosis.exit').read_text(encoding='utf-8-sig').strip()=='0'
    log=(scratch/'cauldron_gate_packet_diagnosis.gradle.log').read_text(encoding='utf-8-sig')
    assert 'BUILD SUCCESSFUL' in log and '(1 tests)' in log and 'entries=3' in log
    native=log[log.index('FINAL19_CAULDRON_GATE_DIAGNOSTIC_START'):]
    assert not any(k in native for k in ['/WARN]','/ERROR]','Failed to handle packet','Leak:'])
    package='src/main/java/com/diskree/achievetodo/injection/mixin/main/'
    accessor='''package com.diskree.achievetodo.injection.mixin.main;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractCauldronBlock.class)
public interface AbstractCauldronBlockAccessor {

    @Accessor("interactions")
    CauldronInteraction.Dispatcher achievetodo$getInteractions();
}
'''
    gate='''package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeCauldronGateMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void lockCauldronInteraction(
        ServerPlayer player,
        Level level,
        ItemStack stack,
        InteractionHand hand,
        BlockHitResult hit,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        // Vanilla skips the block interaction for deliberate secondary item use.
        if (player.isSecondaryUseActive() &&
            (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty())
        ) {
            return;
        }
        var pos = hit.getBlockPos();
        var block = level.getBlockState(pos).getBlock();
        if (!(block instanceof AbstractCauldronBlock)) {
            return;
        }
        var interactions = ((AbstractCauldronBlockAccessor) block).achievetodo$getInteractions();
        if (interactions.get(stack) != CauldronInteraction.DEFAULT &&
            (AchieveToDoMod.isTargetInLockedLandmark(player, level, pos) ||
                AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_CAULDRON))
        ) {
            // End the entire server interaction before trigger emission or item fallback.
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
'''
    config='src/main/resources/achievetodo.mixins.json'
    original=(ROOT/config).read_text(encoding='utf-8')
    assert original.count('    "main.AbstractCauldronBlockMixin",')==1
    assert original.count('    "main.ServerPlayerEntityMixin",')==1
    changed=original.replace('    "main.AbstractCauldronBlockMixin",','    "main.AbstractCauldronBlockAccessor",\n    "main.AbstractCauldronBlockMixin",').replace('    "main.ServerPlayerEntityMixin",','    "main.ServerPlayerEntityMixin",\n    "main.ServerPlayerGameModeCauldronGateMixin",')
    proposals={package+'AbstractCauldronBlockAccessor.java':accessor,package+'ServerPlayerGameModeCauldronGateMixin.java':gate,config:changed}
    output=BASE/'cauldron_gate_production_fix_proposal_sources'
    patch=[];pre={}
    for name,content in proposals.items():
        path=ROOT/name
        if name!=config:assert not path.exists()
        before=path.read_text(encoding='utf-8') if path.exists() else ''
        pre[name]=sha(path) if path.exists() else 'ABSENT'
        patch.extend(difflib.unified_diff(before.splitlines(keepends=True),content.splitlines(keepends=True),fromfile='a/'+name if path.exists() else '/dev/null',tofile='b/'+name))
        proposed=output/name;proposed.parent.mkdir(parents=True,exist_ok=True);proposed.write_text(content,encoding='utf-8',newline='\n')
    path=BASE/'cauldron_gate_production_fix_proposal.patch';path.write_text(''.join(patch),encoding='utf-8',newline='\n')
    inventory={'classification':'PRODUCTION_BUG','diagnosticRunId':evidence['runId'],'patchPath':path.relative_to(ROOT).as_posix(),'patchSha256':sha(path),'productionPreconditions':pre,'changedProductionFiles':list(proposals),'patchApplied':False,'proposalCompile':'PENDING','originalCauldronMixinSha256':sha(ROOT/(package+'AbstractCauldronBlockMixin.java'))}
    (BASE/'cauldron_gate_production_fix_proposal.json').write_text(json.dumps(inventory,indent=2)+'\n',encoding='utf-8')
    print('Review-only cauldron proposal SHA-256='+sha(path))

if __name__=='__main__':main()
