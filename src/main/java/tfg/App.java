package tfg;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;

public final class App {

private static final String TOKEN_DISCORD = Secrets.DISCORD_TOKEN;
	
  public static void main(final String[] args) {
    GatewayDiscordClient gateway = DiscordClientBuilder.create(TOKEN_DISCORD)
      .build().login().block();
    ClientPresence presence = ClientPresence.doNotDisturb(ClientActivity.playing("Hello World!"));
    gateway.updatePresence(presence).block();
    gateway.getEventDispatcher().on(MessageCreateEvent.class)
    .subscribe(event -> {
		User usuario = event.getMessage().getAuthor().orElse(null);
		if (!usuario.isBot()) {
      String response = event.getMessage().getContent();
      event.getMessage()
      .getChannel().block()
      .createMessage(response).block();
		}});
    
    gateway.onDisconnect().block();
  }
}
