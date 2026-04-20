import { createClient } from "npm:@supabase/supabase-js@2"
import { S3Client, PutObjectCommand } from "npm:@aws-sdk/client-s3@3.341.0"
import { getSignedUrl } from "npm:@aws-sdk/s3-request-presigner@3.341.0"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

Deno.serve(async (req: Request) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const supabaseUrl = Deno.env.get('SUPABASE_URL')
    const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')
    
    if (!supabaseUrl || !supabaseKey) throw new Error("Internal configuration error")

    const supabase = createClient(supabaseUrl, supabaseKey)

    // 1. Identify User
    const authHeader = req.headers.get('Authorization')
    if (!authHeader) {
      return new Response(JSON.stringify({ error: 'Missing Authorization header' }), { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }
    
    const token = authHeader.replace('Bearer ', '')
    const { data: { user }, error: userError } = await supabase.auth.getUser(token)
    if (userError || !user) {
      return new Response(JSON.stringify({ error: 'Unauthorized: Invalid token' }), { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    const payload = await req.json()
    const { workspace_id, files } = payload

    if (!workspace_id || !files || !Array.isArray(files)) {
      return new Response(JSON.stringify({ error: 'Missing workspace_id or files array' }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 2. Verify workspace membership
    const { data: membership, error: memberError } = await supabase
      .from('workspace_members')
      .select('user_id')
      .eq('workspace_id', workspace_id)
      .eq('user_id', user.id)
      .maybeSingle()

    if (memberError || !membership) {
      return new Response(JSON.stringify({ error: 'Forbidden: You do not have access to this workspace' }), { status: 403, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 3. Setup R2 Client
    const accountId = Deno.env.get('CLOUDFLARE_ACCOUNT_ID')
    const accessKeyId = Deno.env.get('R2_ACCESS_KEY_ID')
    const secretAccessKey = Deno.env.get('R2_SECRET_ACCESS_KEY')
    const bucket = Deno.env.get('R2_BUCKET')
    const customEndpoint = Deno.env.get('R2_ENDPOINT')
    
    const endpoint = customEndpoint || (accountId ? `https://${accountId}.r2.cloudflarestorage.com` : '')

    if (!endpoint || !accessKeyId || !secretAccessKey || !bucket) {
      return new Response(JSON.stringify({ error: 'Storage configuration error' }), { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    const r2Client = new S3Client({
      region: 'auto',
      endpoint: endpoint,
      credentials: {
        accessKeyId: accessKeyId,
        secretAccessKey: secretAccessKey,
      },
    })

    // 4. Batch Generate URLs (Sequential to avoid 502 resource bursts)
    const uploads = []
    for (const f of files) {
      const timestamp = Date.now()
      const uuid = crypto.randomUUID()
      const filePath = `bugs/${workspace_id}/drafts/${user.id}/${timestamp}_${uuid}.${f.extension}`

      const command = new PutObjectCommand({
        Bucket: bucket,
        Key: filePath,
        ContentType: f.mime_type || 'image/jpeg'
      })

      const uploadUrl = await getSignedUrl(r2Client, command, { expiresIn: 900 })
      
      uploads.push({
        draft_id: f.draft_id,
        upload_url: uploadUrl,
        file_path: filePath
      })
    }

    return new Response(
      JSON.stringify({ uploads }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )

  } catch (error: any) {
    console.error("Critical error:", error)
    return new Response(JSON.stringify({ 
      error: error.message, 
      details: error.toString(),
      stack: error.stack 
    }), { 
      status: 500, 
      headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
    })
  }
})
