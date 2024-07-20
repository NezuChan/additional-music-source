package id.my.nezu.plugin.sources;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

public class GrooviesAudioSourceManager implements AudioSourceManager {
    private final HttpInterfaceManager httpInterfaceManager;
    String pattern = "^(http|https)://groovies\\.my\\.id/(artist|track)/([a-zA-Z0-9]+)$";
    Pattern urlPattern = Pattern.compile(pattern);

    public GrooviesAudioSourceManager() {
        this.httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    }

    @Override
    public String getSourceName() {
        return "groovies";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager audioPlayerManager, AudioReference audioReference) {
        Matcher matcher = urlPattern.matcher(audioReference.identifier);

        if (matcher.matches()) {
            String type = matcher.group(2);
            String id = matcher.group(3);

            if (Objects.equals(type, "track")) {
                return this.loadTrack(id);
            } else if (Objects.equals(type, "artist")) {
                return this.loadArtist(id);
            }

        }

        if (audioReference.identifier.startsWith("grsearch:")) {
            return this.loadSearch(audioReference.identifier.substring("grsearch:".length()).trim());
        }

        return null;
    }

    public AudioItem loadTrack(String id) {
        try (HttpInterface httpInterface = getHttpInterface()) {
            HttpUriRequest httpGetTrackInfoRequest = new HttpGet(
                    String.format("https://api.groovies.my.id/tracks/%s", id)
            );

            try (CloseableHttpResponse TrackInfoResponse = httpInterface.execute(httpGetTrackInfoRequest)) {
                if (!HttpClientTools.isSuccessWithContent(TrackInfoResponse.getStatusLine().getStatusCode())) {
                    return AudioReference.NO_TRACK;
                }

                JsonBrowser TrackInfo = JsonBrowser.parse(TrackInfoResponse.getEntity().getContent()).get("data");
                return new GrooviesAudioTrack(
                        new AudioTrackInfo(
                                TrackInfo.get("title").safeText(),
                                TrackInfo.get("artists").index(0).get("name").safeText(),
                                TrackInfo.get("duration").asLong(0),
                                TrackInfo.get("id").safeText(),
                                false,
                                String.format("https://groovies.my.id/track/%s", TrackInfo.get("id").safeText()),
                                String.format("https://cdn.groovies.my.id/512x512/%s", TrackInfo.get("images").index(0).safeText()),
                                TrackInfo.get("isrc").safeText()
                        ),
                        this
                );
            }
        } catch (Exception e) {
            throw new FriendlyException("Error occurred when extracting track info.", SUSPICIOUS, e);
        }
    }

    public AudioItem loadSearch(String query) {
        try (HttpInterface httpInterface = getHttpInterface()) {
            HttpPost httpPostQueryRequest = new HttpPost("https://api.groovies.my.id/tracks");

            httpPostQueryRequest.setHeader("Content-Type", "application/json");
            httpPostQueryRequest.setEntity(
                    new StringEntity(
                            new JSONObject()
                                    .put("query", query)
                                    .toString()
                    )
            );

            try (CloseableHttpResponse ArtistInfoResponse = httpInterface.execute(httpPostQueryRequest)) {
                JsonBrowser ResultInfo = JsonBrowser.parse(ArtistInfoResponse.getEntity().getContent()).get("data");
                List<AudioTrack> tracks = new ArrayList<>();

                for (JsonBrowser TrackInfo: ResultInfo.get("tracks").values()) {
                    tracks.add(
                            new GrooviesAudioTrack(
                                    new AudioTrackInfo(
                                            TrackInfo.get("title").safeText(),
                                            TrackInfo.get("artists").index(0).get("name").safeText(),
                                            TrackInfo.get("duration").asLong(0),
                                            TrackInfo.get("id").safeText(),
                                            false,
                                            String.format("https://groovies.my.id/track/%s", TrackInfo.get("id").safeText()),
                                            String.format("https://cdn.groovies.my.id/512x512/%s", TrackInfo.get("images").index(0).safeText()),
                                            TrackInfo.get("isrc").safeText()
                                    ),
                                    this
                            )
                    );
                }

                if (tracks.isEmpty()) {
                    return AudioReference.NO_TRACK;
                }

                return new BasicAudioPlaylist(String.format("Loaded result for %s", query), tracks, null, true);
            }
        } catch (Exception e) {
            throw new FriendlyException("Error occurred when extracting track info.", SUSPICIOUS, e);
        }
    }

    public AudioItem loadArtist(String id) {
        try (HttpInterface httpInterface = getHttpInterface()) {
            HttpUriRequest httpGetArtistInfoRequest = new HttpGet(
                    String.format("https://api.groovies.my.id/artists/%s?with_tracks=true", id)
            );

            try (CloseableHttpResponse ArtistInfoResponse = httpInterface.execute(httpGetArtistInfoRequest)) {
                JsonBrowser ArtistInfo = JsonBrowser.parse(ArtistInfoResponse.getEntity().getContent()).get("data");
                List<AudioTrack> tracks = new ArrayList<>();

                String nextUrl = ArtistInfo.get("meta").get("next").text();

                for (JsonBrowser TrackInfo: ArtistInfo.get("tracks").values()) {
                    tracks.add(
                            new GrooviesAudioTrack(
                                new AudioTrackInfo(
                                            TrackInfo.get("title").safeText(),
                                            ArtistInfo.get("name").safeText(),
                                            TrackInfo.get("duration").asLong(0),
                                            TrackInfo.get("id").safeText(),
                                            false,
                                            String.format("https://groovies.my.id/track/%s", TrackInfo.get("id").safeText()),
                                            String.format("https://cdn.groovies.my.id/512x512/%s", TrackInfo.get("images").index(0).safeText()),
                                            TrackInfo.get("isrc").safeText()
                                        ),
                                this
                        )
                    );
                }

                while (nextUrl != null) {
                    HttpUriRequest httpGetNextTracksRequest = new HttpGet(String.format("%s&with_tracks=true", nextUrl));
                    try (CloseableHttpResponse NextArtistInfoResponse = httpInterface.execute(httpGetNextTracksRequest)) {
                        JsonBrowser NextArtistInfo = JsonBrowser.parse(NextArtistInfoResponse.getEntity().getContent()).get("data");
                        nextUrl = NextArtistInfo.get("next").text();
                        for (JsonBrowser TrackInfo: NextArtistInfo.get("tracks").values()) {
                            tracks.add(
                                    new GrooviesAudioTrack(
                                            new AudioTrackInfo(
                                                    TrackInfo.get("title").safeText(),
                                                    NextArtistInfo.get("name").safeText(),
                                                    TrackInfo.get("duration").asLong(0),
                                                    TrackInfo.get("id").safeText(),
                                                    false,
                                                    String.format("https://groovies.my.id/track/%s", TrackInfo.get("id").safeText()),
                                                    String.format("https://cdn.groovies.my.id/512x512/%s", TrackInfo.get("images").index(0).safeText()),
                                                    TrackInfo.get("isrc").safeText()
                                            ),
                                            this
                                    )
                            );
                        }
                    }
                }

                if (tracks.isEmpty()) {
                    return AudioReference.NO_TRACK;
                }

                return new BasicAudioPlaylist(ArtistInfo.get("name").safeText(), tracks, null, false);
            }
        } catch (Exception e) {
            throw new FriendlyException("Error occurred when extracting track info.", SUSPICIOUS, e);
        }
    }

    @Override
    public boolean isTrackEncodable(AudioTrack audioTrack) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack audioTrack, DataOutput dataOutput) {
        // No extra information to save
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo audioTrackInfo, DataInput dataInput) {
        return new GrooviesAudioTrack(audioTrackInfo, this);
    }

    @Override
    public void shutdown() {
        // Nothing to shut down
    }

    public HttpInterface getHttpInterface() {
        return httpInterfaceManager.getInterface();
    }
}
