package appeng.helpers;

import appeng.api.stacks.GenericStack;

import java.util.List;

public interface ICraftingGridMenu {
    void setAutoCraftingJobs(List<AutoCraftEntry> entries);

    record AutoCraftEntry(GenericStack what, List<Long> amounts) {
    }
}
