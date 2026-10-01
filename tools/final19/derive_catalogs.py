"""Derive frozen FINAL19 catalogs; no observed fields or status promotions."""
import hashlib, json, pathlib, zipfile
ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT/'src/test/resources/final19_certification'
def main():
    audit=json.loads((ROOT/'reference/phase_a_planning/final19/pre26_2_final19_audit.json').read_text(encoding='utf-8-sig'))
    OUT.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(ROOT/'reference/phase_a_preservation/files/final/bacap.zip') as archive:
        families={}
        for entry in audit['entries']:
            raw=archive.read(entry['sourcePath'])
            assert hashlib.sha256(raw).hexdigest()==entry['frozenSha256']
            definition=json.loads(raw)
            family=entry['sharedRootCauseId']
            f=families.setdefault(family,{'family':family,'stage':'FINAL19','sources':[],'cases':[]})
            f['sources'].append({'advancementId':entry['advancementId'],'sourcePath':entry['sourcePath'],'frozenSha256':entry['frozenSha256'],
                                'requirements':entry['exactRequirementGroups'],'auditedConvertedDefinition':entry['freshConverterDefinition']})
            for group in entry['plannedRequirementWitnesses']:
                row={'advancementId':entry['advancementId'],'requirementGroup':group['groupIndex'],
                     'alternatives':group['criterionAlternatives'],'criterion':group['plannedWitnessCriterion'],
                     'expectedCriterion':group['criterionDefinition'], 'retainedHistoricalProof':group['alreadySatisfiedByAcceptedReceipt']}
                if family=='WORLDGEN_HOLDERSET_CONTEXT' and entry['advancementId'].endswith('/travelling_bard'):
                    spec=group['criterionDefinition']['conditions']['location'][0]['predicate']['biomes']
                    selected=spec[0] if isinstance(spec,list) else spec
                    row['selectedBiome']=selected if ':' in selected else 'minecraft:'+selected
                    row['selectedDimension']='minecraft:the_nether' if row['selectedBiome'].split(':')[1] in ['nether_wastes','warped_forest','crimson_forest','soul_sand_valley','basalt_deltas'] else 'minecraft:the_end' if group['plannedWitnessCriterion']=='the_end' else 'minecraft:overworld'
                if group.get('enchantmentWitness'):row['enchantmentWitness']=group['enchantmentWitness']
                f['cases'].append(row)
        for family,value in families.items():
            (OUT/(family.lower()+'_catalog.json')).write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    print('Derived 7 catalogs: 19 IDs / 79 groups. No certification change.')
if __name__=='__main__':main()
