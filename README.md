# Sift Farlands

Genuine **Far Lands** in The Sift dimension — produced by real 32-bit integer
overflow, not by a hand-built wall.

| | |
|---|---|
| Minecraft | 26.3 (Fabric) |
| Requires | The Sift 1.1.2+, Fabric API, Fabric Loader 0.19.5+, Java 25 |
| Far Lands begin | `max(abs(x), abs(z)) >= 3,081,840` |
| License | MIT |

## Overview

The Sift (`the_sift:the_sift`) generates its terrain from a data-driven
`noise_settings` + `density_function` pipeline. Its `xz_scale` values are
between 0.06 and 0.96, so the Perlin lattice coordinates never come anywhere
near the `2^31` integer boundary — there is simply nothing to overflow. This
mod does two things:

1. **Removes the Beta 1.8 modulo patch** from `GradientNoise.wrap`, so a
   saturated lattice coordinate is no longer folded back into range (this is
   exactly what `-DMC_DEBUG_ENABLE_FARLANDS` skips).
2. **Accelerates The Sift's own noises past the boundary**, but only outside
   `3,081,840` blocks. Inside that radius every noise keeps its original
   scale, so normal terrain (and the vanilla Overworld) is untouched.

The result is the classic Far Lands life cycle: extruded walls and columns at
the boundary, layer-by-layer degradation, and finally `±Infinity` collapsing
to `NaN` density, leaving only the dimension's default fluid.

## How it works

The mod is three mixins.

### `GradientNoiseMixin`

Injects at the head of `GradientNoise.wrap(double)` and returns the value
unchanged. Vanilla's `wrap` is the Far Lands fix:

```java
if (DEBUG_ENABLE_FARLANDS) return v;
if (-2^24 <= v && v < 2^24) return v;
return v - floor(v / 2^25 + 0.5) * 2^25;   // modulo patch
```

With the patch removed, the later `(int)` cast saturates again.

### `NoiseFunctionMixin`

Redirects the `xzScale` reads inside `NoiseFunction.compileSampler`. For
noises in the `the_sift` namespace the value is passed through **negated**
(`-xzScale`) as a marker; every other sampler keeps its normal scale.

### `NoiseFunctionSamplerMixin`

Intercepts `NoiseFunction$Sampler.sampleValue` and `sampleVolume` — the two
paths the density compiler uses. A negative (marked) scale is turned into a
real scale on every sample:

```java
if (max(abs(x), abs(z)) >= FARLANDS_START) return 2737.648; // forced
else                                       return -rawScale; // original
```

`sampleVolume` uses the volume's `minBlockX/maxBlockX/minBlockZ/maxBlockZ`
to decide. Because the decision happens per sample, nothing outside the
radius changes.

### Why 2737.648

The steepest octave in The Sift is `2^-4` (measured: 43.55 noise units per
block at an `xzScale` of 684.412). Vanilla terrain advances **171.103** units
per block — the number that makes `171.103 × 12,550,824 ≈ 2^31`:

```
171.103 / 2^-4 = 171.103 × 16 = 2737.648
```

Combined with the noise's own ~1.8% normalization factor, that puts the
overflow onset at `x ≈ 3.08M` while keeping the vanilla per-block rate.

### The overflow chain

```
wrap(x) no longer folds the value
  → Perlin lattice coordinate exceeds 2^31
  → Mth.floor saturates at Int.MAX_VALUE
  → frac = x - Int.MAX grows ~700 per block
  → the lerp extrapolates instead of interpolating
  → Far Lands walls / columns
  → further out, summed contributions reach ±Infinity
  → Infinity - Infinity = NaN density
  → no solid blocks remain; only the dimension's default fluid
```

## Installation

1. Install Fabric Loader for Minecraft 26.3 and Fabric API.
2. Install The Sift 1.1.2 or newer.
3. Drop `sift-farlands-1.0.0.jar` into `mods/`.

The vanilla Overworld and any world without The Sift dimension are unaffected.

## Build

```bash
./gradlew build
```

Output: `build/libs/sift-farlands-1.0.0.jar`

Requires JDK 25.

### Minecraft 26.3 / Loom notes

Minecraft 26.1+ ships unobfuscated, which changes the Loom setup:

- the plugin id must be `net.fabricmc.fabric-loom`
- there is **no** `mappings` dependency (Yarn has no 26.3 build and Mojang no
  longer publishes ProGuard mappings)
- dependencies use `implementation`, not `modImplementation`

## Known behaviour

- There is a hard seam at `3,081,840`: the scale jumps from the biome's own
  value to `2737.648` in one step, so the boundary itself is a cliff.
- Chunks generated before the mod was installed keep their old terrain.
- The visible structure depends on which of The Sift's noises is sampled at
  that spot; different regions can look like walls, columns, or a mix.
- The mod logs a `[diag] max noise input = …` line every 10 seconds; it is
  only diagnostic output and can be removed from `SiftFarlands`.

## 中文说明

这是一个让 **The Sift** 维度真正出现边境之地的 Fabric 模组（Minecraft 26.3）。

- 它不画墙、不伪造地形：先去掉 Beta 1.8 的取模补丁（`GradientNoise.wrap`），
  再让 Sift 自己的噪声在 **|x| 或 |z| ≥ 3,081,840** 之后以原版速率
  （171.103 单位/格）推进，最终把 `2^31` 的整数转换打爆。
- 半径以内（包括原版主世界）所有噪声保持原样，地形不会改变。
- 越过边界后依次出现：延伸的墙/柱 → 分层退化 → 密度变成 `NaN` →
  世界只剩该维度的默认流体（岩浆）。
- 边界处是硬接缝（缩放瞬间跳变），这是 `if POS >` 方案固有的现象。

构建：`./gradlew build`，产物在 `build/libs/sift-farlands-1.0.0.jar`。

## Credits

- **The Sift** by Mielon — this is a fan-made add-on, not affiliated with or
  endorsed by the original author.
- The Far Lands mechanism follows vanilla Minecraft's own integer-overflow
  behaviour (Beta 1.8 and earlier).

## License

MIT — see [LICENSE](LICENSE).
