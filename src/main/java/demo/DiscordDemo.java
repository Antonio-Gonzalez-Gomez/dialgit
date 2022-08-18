package demo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import managers.Secrets;

//Demo para la API de Discord
public final class DiscordDemo {
    private static final String TOKEN = Secrets.DISCORD_TOKEN;
    private static final String GUILD_ID = Secrets.DISCORD_TEST_GUILD;
    private static final String CHANNEL_ID = Secrets.DISCORD_TEST_CHANNEL;
    private static final Logger log = LogManager.getLogger();
    
    public static void main(final String[] args) {
        GatewayDiscordClient gateway = DiscordClientBuilder.create(TOKEN)
            .build().login().block();
        log.debug("Launching Discord demo at server with ID: " + GUILD_ID);
        gateway.getGuildById(Snowflake.of(GUILD_ID)).block().getChannelById(Snowflake.of(CHANNEL_ID))
            .cast(MessageChannel.class).block().createMessage("Hello World!").block();
        gateway.logout().block();
    }
}
