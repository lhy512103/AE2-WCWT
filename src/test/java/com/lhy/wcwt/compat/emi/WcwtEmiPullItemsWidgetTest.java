package com.lhy.wcwt.compat.emi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import org.junit.jupiter.api.Test;

class WcwtEmiPullItemsWidgetTest {
    @Test
    void keepsItsSpotWhenNothingElseIsThere() {
        List<Widget> widgets = new ArrayList<>();
        var hammer = new WcwtEmiPullItemsWidget(100, 40, null, widgets);
        widgets.add(hammer);

        assertEquals(new Bounds(100, 40, 12, 12), hammer.getBounds());
    }

    @Test
    void movesUpWhenAButtonAddedAfterItTakesTheSpot() {
        List<Widget> widgets = new ArrayList<>();
        var hammer = new WcwtEmiPullItemsWidget(100, 40, null, widgets);
        widgets.add(hammer);
        widgets.add(fixed(new Bounds(100, 40, 12, 12)));

        assertEquals(new Bounds(100, 26, 12, 12), hammer.getBounds());
    }

    @Test
    void movesToTheNextColumnWhenTheColumnIsFull() {
        List<Widget> widgets = new ArrayList<>();
        var hammer = new WcwtEmiPullItemsWidget(100, 14, null, widgets);
        widgets.add(hammer);
        widgets.add(fixed(new Bounds(100, 14, 12, 12)));
        widgets.add(fixed(new Bounds(100, 0, 12, 12)));

        assertEquals(new Bounds(114, 14, 12, 12), hammer.getBounds());
    }

    @Test
    void touchingButtonsDoNotCountAsOverlap() {
        List<Widget> widgets = new ArrayList<>();
        var hammer = new WcwtEmiPullItemsWidget(100, 40, null, widgets);
        widgets.add(fixed(new Bounds(112, 40, 12, 12)));
        widgets.add(fixed(new Bounds(100, 52, 12, 12)));

        assertEquals(new Bounds(100, 40, 12, 12), hammer.getBounds());
    }

    private static Widget fixed(Bounds bounds) {
        return new Widget() {
            @Override
            public Bounds getBounds() {
                return bounds;
            }

            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            }
        };
    }
}
