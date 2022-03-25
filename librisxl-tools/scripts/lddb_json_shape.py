import json
import os


MAX_STATS = int(os.environ.get('MAX_STATS', '512'))
HARD_MAX_STATS = 8192
STATS_FOR_ALL = {
        # auth
        "hasBiographicalInformation",
        "marc:hasBiographicalOrHistoricalData",
        # "shouldn't" be too many...
        "marc:displayText",
        "part",
}


def reshape(data):
    if '@graph' in data:
        graph = data['@graph']
        thing = graph[1]
        thing['meta'] = graph[0]

        work = thing.get('instanceOf')
        if work and len(work) == 1 and '@id' in work:
            for item in graph[2:]:
                if item.get('@id') == work['@id']:
                    work = thing['instanceOf'] = item
                    break

        return thing, work

    return data, data.get('instanceOf')


def compute_shape(node, index, type_key=None):
    if len(node) == 1 and '@id' in node:
        count_value('@id', node['@id'], index)
        return

    rtype = type_key or node.get('@type')
    if isinstance(rtype, list):
        rtype = '+'.join(rtype)
    shape = index.setdefault(rtype, {})

    for k, vs in node.items():
        if not isinstance(vs, list):
            vs = [vs] # Ignoring dict/list difference for now

        for v in vs:
            if isinstance(v, dict):
                subindex = shape.setdefault(k, {})
                compute_shape(v, subindex)
            else:
                count_value(k, v, shape)


def count_value(k, v, shape):
    stats = shape.setdefault(k, {})
    if isinstance(stats, dict):
        if k in STATS_FOR_ALL and len(stats) < HARD_MAX_STATS or \
                len(stats) < MAX_STATS:
            if not k.startswith('@') and isinstance(v, (str, bool, int, float)):
                v = f'@value {v}'
            stats[v] = stats.setdefault(v, 0) + 1
        else:
            shape[k] = sum(stats.values()) + 1
    else:
        shape[k] = stats + 1


if __name__ == '__main__':
    from time import time
    import sys
    from pathlib import Path

    args = sys.argv[:]
    cmd = args.pop(0)
    if not args:
        print(f'USAGE: {cmd} OUT_DIR', file=sys.stderr)
        sys.exit(1)

    outpath = Path(args.pop(0))
    SUFFIX = '.json'
    if outpath.suffix == SUFFIX:
        outdir = outpath.parent
    else:
        outdir = outpath
        outpath = None
    if not outdir.is_dir():
        outdir.mkdir(parents=True, exist_ok=True)

    index = {}
    work_by_type_index = {}
    instance_index = {}
    work_index = {}

    t_last = 0
    cr = '\r'
    for i, l in enumerate(sys.stdin):
        if not l.rstrip():
            continue
        if isinstance(l, bytes):
            l = l.decode('utf-8')

        t_now = time()
        if t_now - t_last > 2:
            t_last = t_now
            print(f'{cr}At: {i + 1:,}', end='', file=sys.stderr)

        try:
            data = json.loads(l)
            thing, work = reshape(data)
            compute_shape(thing, index)
            if work:
                compute_shape(thing, instance_index, type_key='Instance')
                compute_shape(work, work_by_type_index)
                compute_shape(work, work_index, type_key='Work')

        except (ValueError, AttributeError) as e:
            print(f'ERROR at: {i} in data:', file=sys.stderr)
            print(l, file=sys.stderr)
            print(e, file=sys.stderr)

    print(f'{cr}Total: {i + 1:,}', file=sys.stderr)

    def output(index, fpath):
        with fpath.open('w') as f:
            json.dump(index, f, indent=2, ensure_ascii=False)
        print(f'Wrote: {fpath}', file=sys.stderr)

    if outpath:
        output(index, outpath)
    else:
        to_outfile = lambda name: (outdir / name).with_suffix(SUFFIX)
        output(index, to_outfile('instance_shapes_by_type'))
        output(instance_index, to_outfile('instance_shapes'))
        output(work_by_type_index, to_outfile('work_shapes_by_type'))
        output(work_index, to_outfile('work_shapes'))
