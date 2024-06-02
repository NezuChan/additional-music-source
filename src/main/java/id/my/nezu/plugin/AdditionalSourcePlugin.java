package id.my.nezu.plugin;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import dev.arbjerg.lavalink.api.AudioPlayerManagerConfiguration;
import id.my.nezu.plugin.sources.GrooviesAudioSourceManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdditionalSourcePlugin implements AudioPlayerManagerConfiguration {
    private static final Logger log = LoggerFactory.getLogger(AdditionalSourcePlugin.class);

    public AdditionalSourcePlugin() {
        log.info("Loading AdditionalMusicSource plugin...");
    }

    @NotNull
    @Override
    public AudioPlayerManager configure(@NotNull AudioPlayerManager manager) {
        log.info("Registering Groovies audio source manager...");
        manager.registerSourceManager(
                new GrooviesAudioSourceManager()
        );

        return manager;
    }
}
