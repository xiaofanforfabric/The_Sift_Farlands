package mielon.siftfarlands.mixin;

import mielon.siftfarlands.SiftFarlands;
import net.minecraft.world.level.levelgen.densityfunction.generator.NoiseFunction;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes The Sift's own noises advance at the vanilla terrain rate.
 *
 * <p>Vanilla terrain noise advances 171.103 units per block, which is why the
 * integer cast inside the Perlin sampler lands exactly on 2^31 at the classic
 * Far Lands boundary (12,550,824). The Sift's noises use {@code xz_scale}
 * values between 0.06 and 0.96 and peaked at ~0.28 units/block, three orders
 * of magnitude too short to ever overflow.
 *
 * <p>The redirect is scoped to noises in the {@code the_sift} namespace so
 * vanilla dimensions keep their own {@code xz_scale}. For Sift noises the
 * scale is negated instead of replaced: the sampler mixin reads that marker
 * and decides per sample whether the forced scale applies (see
 * {@link SiftFarlands#scaleFor(double, int, int)}).
 *
 * <p>{@code compileSampler} reads {@code xzScale} three times, once per sampler
 * flavour, so a single field redirect covers all of them.
 */
@Mixin(NoiseFunction.class)
public abstract class NoiseFunctionMixin {

	@Redirect(
			method = "compileSampler",
			at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
					target = "Lnet/minecraft/world/level/levelgen/densityfunction/generator/NoiseFunction;xzScale:D"))
	private double sift$forceVanillaScale(NoiseFunction self) {
		boolean isSiftNoise = self.noise().unwrapKey()
				.map(key -> "the_sift".equals(key.identifier().getNamespace()))
				.orElse(false);
		return isSiftNoise ? -self.xzScale() : self.xzScale();
	}
}
