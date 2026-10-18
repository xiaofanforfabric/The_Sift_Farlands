package mielon.siftfarlands.mixin;

import mielon.siftfarlands.SiftFarlands;
import net.minecraft.world.level.levelgen.synth.GradientNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Removes the Beta 1.8 Far Lands fix from the Perlin sampler.
 *
 * <p>Vanilla {@code wrap} is:
 * <pre>
 *   if (DEBUG_ENABLE_FARLANDS) return v;
 *   if (-2^24 &lt;= v &lt; 2^24) return v;
 *   return v - floor(v / 2^25 + 0.5) * 2^25;   // the modulo patch
 * </pre>
 * The last line folds the input into a range where the later {@code (int)} cast
 * cannot overflow. Returning {@code v} unchanged lets that cast saturate again,
 * which is what produced the Far Lands.
 *
 * <p>This hook never decides <em>where</em> Far Lands appear. It only removes the
 * guard; whether the cast overflows depends on how many noise units the
 * dimension accumulates per block.
 */
@Mixin(GradientNoise.class)
public abstract class GradientNoiseMixin {

	@Inject(method = "wrap(D)D", at = @At("HEAD"), cancellable = true)
	private static void sift$removeModuloPatch(double value,
											   CallbackInfoReturnable<Double> cir) {
		SiftFarlands.recordInput(value);
		cir.setReturnValue(value);
	}
}
