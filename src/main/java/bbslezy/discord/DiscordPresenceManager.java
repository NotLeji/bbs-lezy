package bbslezy.discord;

import bbslod.LodSettings;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;

import net.minecraft.client.MinecraftClient;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DiscordPresenceManager
{
    public static final DiscordPresenceManager INSTANCE = new DiscordPresenceManager();

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String STATE = "BBS Film Studio";
    private static final String IDLE_DETAILS = "In Minecraft";
    public static final String DEFAULT_APPLICATION_ID = "383226320970055681";
    private static final int IDLE_REFRESH_INTERVAL = 600;

    private final BlockingQueue<PresenceTask> tasks = new LinkedBlockingQueue<>();
    private Thread workerThread;
    private DiscordIpcClient client;
    private String connectedApplicationId = "";
    private boolean gameRunning;
    private boolean bbsUiOpen;
    private String details = "";
    private long gameSessionStart;
    private long sessionStart;
    private int idleRefreshTicks;
    private boolean loggedInvalidAppId;

    private DiscordPresenceManager()
    {}

    public void init()
    {
        if (this.workerThread != null)
        {
            return;
        }

        this.workerThread = new Thread(this::workerLoop, "BBS-Discord-Presence");
        this.workerThread.setDaemon(true);
        this.workerThread.start();

        this.gameRunning = true;
        this.gameSessionStart = System.currentTimeMillis() / 1000L;
        this.idleRefreshTicks = 0;

        if (this.isEnabled())
        {
            this.enqueue(PresenceTask.set(IDLE_DETAILS, this.gameSessionStart));
        }
    }

    public void shutdown()
    {
        this.gameRunning = false;
        this.bbsUiOpen = false;
        this.enqueue(PresenceTask.clear());

        if (this.workerThread != null)
        {
            this.workerThread.interrupt();
            this.workerThread = null;
        }
    }

    public void onSettingsChanged()
    {
        if (!this.isEnabled())
        {
            this.enqueue(PresenceTask.clear());

            return;
        }

        this.enqueue(PresenceTask.reconnect());

        if (!this.gameRunning)
        {
            return;
        }

        if (this.bbsUiOpen)
        {
            this.enqueue(PresenceTask.set(this.details, this.sessionStart));
        }
        else
        {
            this.enqueue(PresenceTask.set(IDLE_DETAILS, this.gameSessionStart));
        }
    }

    public void tick()
    {
        if (!this.isEnabled() || !this.gameRunning)
        {
            return;
        }

        UIBaseMenu menu = UIScreen.getCurrentMenu();

        if (menu != null)
        {
            if (!this.bbsUiOpen)
            {
                this.bbsUiOpen = true;
                this.sessionStart = System.currentTimeMillis() / 1000L;
                this.details = this.resolveDetails(menu);
                this.enqueue(PresenceTask.set(this.details, this.sessionStart));
            }
            else
            {
                String nextDetails = this.resolveDetails(menu);

                if (!nextDetails.equals(this.details))
                {
                    this.details = nextDetails;
                    this.enqueue(PresenceTask.set(this.details, this.sessionStart));
                }
            }

            return;
        }

        if (this.bbsUiOpen)
        {
            this.bbsUiOpen = false;
            this.details = "";
            this.enqueue(PresenceTask.set(IDLE_DETAILS, this.gameSessionStart));

            return;
        }

        this.idleRefreshTicks++;

        if (this.idleRefreshTicks >= IDLE_REFRESH_INTERVAL)
        {
            this.idleRefreshTicks = 0;
            this.enqueue(PresenceTask.set(IDLE_DETAILS, this.gameSessionStart));
        }
    }

    private String getApplicationId()
    {
        if (LodSettings.discordAppId != null)
        {
            String id = LodSettings.discordAppId.get();

            if (id != null && !id.trim().isEmpty())
            {
                return id.trim();
            }
        }

        return DEFAULT_APPLICATION_ID;
    }

    private boolean isEnabled()
    {
        return LodSettings.discordEnabled != null && LodSettings.discordEnabled.get();
    }

    private boolean isValidApplicationId(String applicationId)
    {
        if (applicationId == null || applicationId.isEmpty())
        {
            return false;
        }

        if (applicationId.length() < 17 || applicationId.length() > 20)
        {
            return false;
        }

        for (int i = 0; i < applicationId.length(); i++)
        {
            if (!Character.isDigit(applicationId.charAt(i)))
            {
                return false;
            }
        }

        return true;
    }

    private String resolveDetails(UIBaseMenu menu)
    {
        if (menu instanceof UIDashboard dashboard)
        {
            UIDashboardPanel panel = dashboard.getPanels().panel;

            if (panel instanceof UIFilmPanel filmPanel)
            {
                Film film = filmPanel.getData();

                if (film != null && !film.getId().isEmpty())
                {
                    return "Editing film · " + film.getId();
                }

                return "Film Editor";
            }

            return "Dashboard";
        }

        return STATE;
    }

    private void enqueue(PresenceTask task)
    {
        this.tasks.offer(task);
    }

    private void workerLoop()
    {
        while (!Thread.currentThread().isInterrupted())
        {
            try
            {
                PresenceTask task = this.tasks.poll(1L, TimeUnit.SECONDS);

                if (task == null)
                {
                    continue;
                }

                this.applyTask(task);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }

        this.disconnectClient();
    }

    private void applyTask(PresenceTask task)
    {
        if (task.clear)
        {
            this.clearPresence();

            return;
        }

        if (task.reconnect)
        {
            this.disconnectClient();

            return;
        }

        if (!MinecraftClient.getInstance().isRunning())
        {
            return;
        }

        if (!this.isEnabled())
        {
            return;
        }

        String applicationId = this.getApplicationId();

        if (!this.isValidApplicationId(applicationId))
        {
            if (!this.loggedInvalidAppId)
            {
                this.loggedInvalidAppId = true;
                LOGGER.debug("Discord Rich Presence: invalid application ID \"{}\".", applicationId);
            }

            return;
        }

        if (this.client == null || !this.client.isConnected() || !applicationId.equals(this.connectedApplicationId))
        {
            this.disconnectClient();
            this.client = new DiscordIpcClient(applicationId);

            if (!this.client.connect())
            {
                this.client = null;
                this.connectedApplicationId = "";

                return;
            }

            this.connectedApplicationId = applicationId;
        }

        this.client.setActivity(task.details, STATE, task.startTimestamp);
    }

    private void clearPresence()
    {
        if (this.client != null)
        {
            this.client.clearActivity();
            this.client.disconnect();
            this.client = null;
            this.connectedApplicationId = "";
        }
    }

    private void disconnectClient()
    {
        if (this.client != null)
        {
            this.client.disconnect();
            this.client = null;
            this.connectedApplicationId = "";
        }
    }

    private static final class PresenceTask
    {
        private final boolean clear;
        private final boolean reconnect;
        private final String details;
        private final long startTimestamp;

        private PresenceTask(boolean clear, boolean reconnect, String details, long startTimestamp)
        {
            this.clear = clear;
            this.reconnect = reconnect;
            this.details = details;
            this.startTimestamp = startTimestamp;
        }

        private static PresenceTask set(String details, long startTimestamp)
        {
            return new PresenceTask(false, false, details, startTimestamp);
        }

        private static PresenceTask clear()
        {
            return new PresenceTask(true, false, "", 0L);
        }

        private static PresenceTask reconnect()
        {
            return new PresenceTask(false, true, "", 0L);
        }
    }
}
