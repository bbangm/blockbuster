package mchorse.blockbuster.client.gui.elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import mchorse.blockbuster.client.gui.GuiDirectorNew;
import mchorse.blockbuster.common.tileentity.director.Replay;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Director block's cast GUI
 *
 * This class is responsible for rendering scroll list with director block's
 * cast and execute different actions (edit, remove) when player hits the
 * button.
 */
@SideOnly(Side.CLIENT)
public class GuiReplays extends GuiScrollPane
{
    private final int span = 20;
    private int selected = -1;

    /**
     * Comparator, sorts by alphabet in ascending order
     */
    public static final Comparator<Entry> ALPHA = new Comparator<Entry>()
    {
        @Override
        public int compare(Entry a, Entry b)
        {
            return a.name.compareTo(b.name);
        }
    };

    private String noCast = I18n.format("blockbuster.director.no_cast");

    /* Input data */
    private GuiDirectorNew parent;

    /**
     * List of entries
     *
     * This list is being compiled in the setCast method, basically this list is
     * used for rendering of the rows and storing the data for edit and remove
     * actions.
     */
    public List<Entry> entries = new ArrayList<Entry>();

    public GuiReplays(GuiDirectorNew parent)
    {
        this.parent = parent;
    }

    /**
     * Compile entries out of passed actors UUIDs
     */
    public void setCast(List<Replay> replays)
    {
        this.scrollY = 0;
        this.scrollHeight = replays.size() * this.span;
        this.entries.clear();

        for (int i = 0; i < replays.size(); i++)
            this.entries.add(new Entry(i, replays.get(i)));

        this.entries.sort(ALPHA);
    }

    /* Handling */

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (!this.isInside(mouseX, mouseY) || mouseX > this.x + this.w - 8)
        {
            return;
        }

        int index = (mouseY - this.y + this.scrollY) / this.span;

        if (!this.entries.isEmpty() && index >= 0 && index < this.entries.size())
        {
            this.parent.setSelected(this.entries.get(index).replay, index);
            this.selected = index;
        }
        else
        {
            this.parent.setSelected(null, -1);
            this.selected = -1;
        }
    }

    /* GUI & drawing */

    @Override
    protected void drawBackground()
    {}

    @Override
    protected void drawPane()
    {
        if (this.entries.size() == 0)
        {
            this.fontRendererObj.drawStringWithShadow(this.noCast, this.x + 2, this.y + 8, 0xffcccccc);
            return;
        }

        for (int i = 0, c = this.entries.size(); i < c; i++)
        {
            int x = this.x + 2;
            int y = this.y + i * this.span;
            boolean flag = i == this.selected;

            Entry entry = this.entries.get(i);
            String name = flag ? "> " + entry.name : entry.name;

            this.fontRendererObj.drawStringWithShadow(name, x, y + 8, flag ? 0xffcccccc : 0xffffffff);
        }
    }

    /**
     * Entry class
     *
     * Basically this represents a slot in the scroll pane
     */
    static class Entry
    {
        public int index;
        public String name;
        public Replay replay;

        public Entry(int index, Replay replay)
        {
            this.index = index;
            this.replay = replay;

            if (!replay.name.isEmpty())
            {
                this.name = replay.name + (replay.id.isEmpty() ? "" : " (" + replay.id + ")");
            }
            else if (!replay.id.isEmpty())
            {
                this.name = replay.id;
            }
            else
            {
                this.name = "Actor";
            }
        }
    }
}