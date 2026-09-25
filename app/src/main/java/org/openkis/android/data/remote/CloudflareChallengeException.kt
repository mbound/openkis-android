package org.openkis.android.data.remote

import java.io.IOException

class CloudflareChallengeException(
    val challengedUrl: String
) : IOException("Cloudflare browser challenge returned for $challengedUrl")
