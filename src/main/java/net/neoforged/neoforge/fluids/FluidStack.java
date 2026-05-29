package net.neoforged.neoforge.fluids;

import net.minecraft.core.Holder;
import net.minecraft.world.level.material.Fluid;

public class FluidStack extends net.minecraftforge.fluids.FluidStack {
    public FluidStack(Fluid fluid, int amount) {
        super(fluid, amount);
    }

    public FluidStack(Holder<Fluid> fluid, int amount) {
        super(fluid.value(), amount);
    }
}
