package demo;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import tfg.Secrets;

public final class DiscordDemo {
    private static final String TOKEN = Secrets.DISCORD_TOKEN;
    private static final String GUILD_ID = Secrets.DISCORD_TEST_GUILD;
    private static final String CHANNEL_ID = Secrets.DISCORD_TEST_CHANNEL;

    public static void main(final String[] args) {
        GatewayDiscordClient gateway = DiscordClientBuilder.create(TOKEN)
            .build().login().block();
        ClientPresence presence = ClientPresence.doNotDisturb(ClientActivity.playing("Testing Discord API!"));
        gateway.updatePresence(presence).block();
        gateway.getGuildById(Snowflake.of(GUILD_ID)).block().getChannelById(Snowflake.of(CHANNEL_ID))
            .cast(MessageChannel.class).block().createMessage("Hello World!").block();
        gateway.logout().block();
    }
}
