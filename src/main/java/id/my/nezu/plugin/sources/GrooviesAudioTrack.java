package id.my.nezu.plugin.sources;

import com.sedmelluq.discord.lavaplayer.container.ogg.OggAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class GrooviesAudioTrack extends DelegatedAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(GrooviesAudioTrack.class);
    private final GrooviesAudioSourceManager sourceManager;

    public GrooviesAudioTrack(AudioTrackInfo trackInfo, GrooviesAudioSourceManager sourceManager) {
        super(trackInfo);
        this.sourceManager = sourceManager;
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new GrooviesAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public void process(LocalAudioTrackExecutor localAudioTrackExecutor) throws Exception {
        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {

            HttpUriRequest httpGetTrackInfoRequest = new HttpGet(
                    String.format("https://api.groovies.my.id/tracks/%s", trackInfo.identifier)
            );

            try (CloseableHttpResponse TrackInfoResponse = httpInterface.execute(httpGetTrackInfoRequest)) {
                JsonBrowser TrackInfo = JsonBrowser.parse(TrackInfoResponse.getEntity().getContent());

                HttpUriRequest httpGetStreamUrlRequest = new HttpGet(
                        String.format("https://api.groovies.my.id/storage-resolver/%s", TrackInfo.get("data").get("files").index(0).get("id").safeText())
                );

                try (CloseableHttpResponse TrackStreamResponse = httpInterface.execute(httpGetStreamUrlRequest)) {
                JsonBrowser TrackStream = JsonBrowser.parse(TrackStreamResponse.getEntity().getContent());
                    log.debug("Starting http track from URL: {}", TrackStream.get("data").get("url").safeText());

                try (PersistentHttpStream inputStream = new PersistentHttpStream(httpInterface, new URI(TrackStream.get("data").get("url").safeText()), Units.CONTENT_LENGTH_UNKNOWN)) {
                        processDelegate(new OggAudioTrack(trackInfo, inputStream), localAudioTrackExecutor);
                    }
                }
            }
        }
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }
}
