"""Read-only V2 CLI. No capture/fallback; draft authority always rejects."""
from pathlib import Path
import argparse
import json
import os
import sys

if __package__:
    from . import distribution_b_v2_contract as c
else:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
    from tools.build import distribution_b_v2_contract as c


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-root', type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument('--external-root', default=os.environ.get('ACHIEVETODO_TEST_INPUTS_DIR'))
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument('--candidate-report', action='store_true')
    modes.add_argument('--approval-only', action='store_true')
    modes.add_argument('--dependencies', action='store_true')
    parser.add_argument('--gradle-user-home', type=Path)
    args = parser.parse_args()
    if args.candidate_report:
        c.require(not any(key in os.environ for key in (c.SELECTOR, c.APPROVAL_FILE,
            'ACHIEVETODO_INPUT_AUTHORITY', 'ACHIEVETODO_ALTERNATIVE_MANIFEST',
            'ACHIEVETODO_SOURCE_ORACLE', 'ACHIEVETODO_SOURCE_ORACLE_FILE')),
            'candidate diagnostic cannot ignore authority selectors')
        result = c.inspect_candidate(args.source_root, args.external_root)
        print(json.dumps(result, sort_keys=True))
    elif args.approval_only:
        result = c.validate_selection(args.source_root)
        c.require(result is not None, 'explicit V2 approval required')
        print(json.dumps({'authority': c.AUTHORITY, 'sourceAuxiliaryBindings': result['sourceAuxiliaryBindings'],
                          'fixturePaths': result['fixturePaths']}, separators=(',', ':')))
    elif args.dependencies:
        print(json.dumps(c.dependencies(args.source_root, args.gradle_user_home)))
    else:
        c.verify_activation(args.source_root, args.external_root)
        print('EXTERNAL_INPUTS_V2_PASS authority='+c.AUTHORITY+' originals=15 raw=10 originalGate=STANDARD_INPUT_BLOCKED')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, KeyError, TypeError, OSError, StopIteration) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
