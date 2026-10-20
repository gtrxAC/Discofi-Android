package fi.gtrxac.discord;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.module.GlideModule;

import java.io.IOException;
import java.io.InputStream;

import fi.gtrxac.bluewap.http.HTTP;

public class MicrocordGlideModule implements GlideModule {
    public void applyOptions(Context context, GlideBuilder builder) {
    }

    public void registerComponents(Context context, Glide glide) {
        glide.register(GlideUrl.class, InputStream.class, new HttpModelLoaderFactory());
    }

    private static class HttpModelLoaderFactory implements ModelLoaderFactory<GlideUrl, InputStream> {
        public ModelLoader<GlideUrl, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new HttpModelLoader();
        }

        public void teardown() {
        }
    }

    private static class HttpModelLoader implements ModelLoader<GlideUrl, InputStream> {
        public DataFetcher<InputStream> getResourceFetcher(GlideUrl model, int width, int height) {
            return new HttpDataFetcher(model.toStringUrl());
        }
    }

    private static class HttpDataFetcher implements DataFetcher<InputStream> {
        private final String url;
        private HTTP request;
        private volatile boolean cancelled;

        HttpDataFetcher(String url) {
            this.url = url;
        }

        public InputStream loadData(Priority priority) throws Exception {
            HTTP http = HTTP.createRequest("GET", url);
            request = http;

            int responseCode = http.getResponseCode();
            if (cancelled) throw new IOException("Cancelled");
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("Image request failed with HTTP " + responseCode + ": " + url);
            }
            return http.getResponseStream();
        }

        public void cleanup() {
            HTTP http = request;
            request = null;
            if (http != null) http.close();
        }

        public String getId() {
            return url;
        }

        public void cancel() {
            cancelled = true;
        }
    }
}
