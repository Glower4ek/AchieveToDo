"""Minimal NBT (Java edition, big-endian, gzip) reader/writer for level.dat surgery in campaign worlds.

Values are kept with their tag ids so a round trip writes byte-identical data:
compound -> dict name -> (tag, value); list -> (element_tag, [values]).
"""
import gzip, struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)


class _R:
    def __init__(self, data):
        self.d, self.i = data, 0

    def take(self, n):
        v = self.d[self.i:self.i + n]
        self.i += n
        return v

    def u(self, fmt):
        n = struct.calcsize(fmt)
        return struct.unpack('>' + fmt, self.take(n))[0]

    def string(self):
        return self.take(self.u('H')).decode('utf-8', 'surrogatepass')


def _read(r, tag):
    if tag == BYTE: return r.u('b')
    if tag == SHORT: return r.u('h')
    if tag == INT: return r.u('i')
    if tag == LONG: return r.u('q')
    if tag == FLOAT: return r.u('f')
    if tag == DOUBLE: return r.u('d')
    if tag == BYTE_ARRAY: return r.take(r.u('i'))
    if tag == STRING: return r.string()
    if tag == LIST:
        et, n = r.u('b'), r.u('i')
        return (et, [_read(r, et) for _ in range(n)])
    if tag == COMPOUND:
        out = {}
        while True:
            t = r.u('b')
            if t == END:
                return out
            name = r.string()
            out[name] = (t, _read(r, t))
    if tag == INT_ARRAY:
        n = r.u('i'); return list(struct.unpack(f'>{n}i', r.take(4 * n)))
    if tag == LONG_ARRAY:
        n = r.u('i'); return list(struct.unpack(f'>{n}q', r.take(8 * n)))
    raise ValueError(f'tag {tag}')


def _write(out, tag, v):
    p = lambda fmt, x: out.append(struct.pack('>' + fmt, x))
    if tag == BYTE: p('b', v)
    elif tag == SHORT: p('h', v)
    elif tag == INT: p('i', v)
    elif tag == LONG: p('q', v)
    elif tag == FLOAT: p('f', v)
    elif tag == DOUBLE: p('d', v)
    elif tag == BYTE_ARRAY: p('i', len(v)); out.append(bytes(v))
    elif tag == STRING:
        b = v.encode('utf-8', 'surrogatepass'); p('H', len(b)); out.append(b)
    elif tag == LIST:
        et, items = v; p('b', et); p('i', len(items))
        for x in items: _write(out, et, x)
    elif tag == COMPOUND:
        for name, (t, x) in v.items():
            p('b', t); _write(out, STRING, name); _write(out, t, x)
        p('b', END)
    elif tag == INT_ARRAY: p('i', len(v)); out.append(struct.pack(f'>{len(v)}i', *v))
    elif tag == LONG_ARRAY: p('i', len(v)); out.append(struct.pack(f'>{len(v)}q', *v))
    else: raise ValueError(f'tag {tag}')


def load(path):
    r = _R(gzip.decompress(open(path, 'rb').read()))
    t = r.u('b'); name = r.string()
    assert t == COMPOUND
    return name, _read(r, COMPOUND)


def dump(path, name, root):
    out = []
    out.append(struct.pack('>b', COMPOUND)); _write(out, STRING, name); _write(out, COMPOUND, root)
    with open(path, 'wb') as f:
        f.write(gzip.compress(b''.join(out)))


def plain(v):
    """Tag-free view for printing."""
    if isinstance(v, dict): return {k: plain(x) for k, (t, x) in v.items()}
    if isinstance(v, tuple) and len(v) == 2 and isinstance(v[1], list): return [plain(x) for x in v[1]]
    if isinstance(v, bytes): return f'<{len(v)} bytes>'
    return v
