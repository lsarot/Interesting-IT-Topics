public class CustomGlide implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {}

    @Override
    public void registerComponents(Context context, Glide glide) {

        RequestQueue queue = new RequestQueue( // params hardcoded from Volley.newRequestQueue()
                new DiskBasedCache(new File(context.getCacheDir(), "volley")),
                new BasicNetwork(new HurlStack())) {

            @Override public <T> Request<T> add(Request<T> request) {
                request.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1));
                return super.add(request);
            }

        };

        queue.start();
        glide.register(GlideUrl.class, InputStream.class, new VolleyUrlLoader.Factory(queue));

    }
}




<meta-data
            android:name="<package-name>.CustomGlide"
            android:value="GlideModule" />

For VolleyUrlLoader you need to add: compile 'com.github.bumptech.glide:volley-integration:1.5.0@aar'





----------------------------------



@GlideModule
public class CustomGlideModule extends AppGlideModule {

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {}

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // set your timeout here
        builder.readTimeout(YOUR_CUSTOM_TIMEOUT, TimeUnit.SECONDS);
        builder.writeTimeout(YOUR_CUSTOM_TIMEOUT, TimeUnit.SECONDS);
        builder.connectTimeout(YOUR_CUSTOM_TIMEOUT, TimeUnit.SECONDS);

        registry.append(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(builder.build()));

    }

}


implementation 'com.squareup.okhttp3:okhttp:3.9.1'
implementation 'com.github.bumptech.glide:glide:4.3.1'
annotationProcessor 'com.github.bumptech.glide:compiler:4.3.1'
implementation 'com.github.bumptech.glide:okhttp3-integration:4.3.1'











