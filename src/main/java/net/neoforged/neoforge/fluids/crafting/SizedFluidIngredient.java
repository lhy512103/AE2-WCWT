package net.neoforged.neoforge.fluids.crafting;

import net.minecraftforge.fluids.FluidStack;

import java.util.List;

public class SizedFluidIngredient {
    private final List<FluidStack> fluids;
    private final int amount;

    public SizedFluidIngredient(List<FluidStack> fluids) {
        this(fluids, fluids.isEmpty() ? 0 : fluids.get(0).getAmount());
    }

    public SizedFluidIngredient(List<FluidStack> fluids, int amount) {
        this.fluids = fluids;
        this.amount = amount;
    }

    public List<FluidStack> getFluids() {
        return fluids;
    }

    public int amount() {
        return amount;
    }
}
