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
    if (!authHeader) throw new Error('Unauthorized')
    
    const token = authHeader.replace('Bearer ', '')
    const { data: { user }, error: userError } = await supabase.auth.getUser(token)
    if (userError || !user) throw new Error('Unauthorized')

    const payload = await req.json()
    const { module_id, version_name, version_code, package_name, file_size } = payload

    if (!module_id || !version_name || !version_code || !package_name) {
      throw new Error('Missing required fields')
    }

    // 2. Fetch module & Workspace Security
    const { data: module, error: moduleError } = await supabase
      .from('modules')
      .select('package_name, workspace_id')
      .eq('id', module_id)
      .single()

    if (moduleError || !module) {
      return new Response(JSON.stringify({ error: 'Module not found' }), { status: 404, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // Verify workspace membership
    const { data: membership, error: memberError } = await supabase
      .from('workspace_members')
      .select('user_id')
      .eq('workspace_id', module.workspace_id)
      .eq('user_id', user.id)
      .maybeSingle()

    if (memberError || !membership) {
      return new Response(JSON.stringify({ error: 'Forbidden: You do not have access to this workspace' }), { status: 403, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 3. Strict Package Name Validation
    if (module.package_name !== package_name) {
      return new Response(JSON.stringify({ error: `Package name mismatch` }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 4. Version Seniority Check
    const { data: latestVersion, error: versionError } = await supabase
      .from('app_versions')
      .select('version_code')
      .eq('module_id', module_id)
      .order('version_code', { ascending: false })
      .limit(1)
      .maybeSingle()

    if (latestVersion && version_code < latestVersion.version_code) {
      return new Response(JSON.stringify({ error: `Version ${version_code} must be higher or equal to current ${latestVersion.version_code}` }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 5. File Size Limit (150MB)
    if (file_size && file_size > 150 * 1024 * 1024) {
      return new Response(JSON.stringify({ error: 'File too large' }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 6. Generate Presigned R2 URL
    const accountId = Deno.env.get('CLOUDFLARE_ACCOUNT_ID')
    const accessKeyId = Deno.env.get('R2_ACCESS_KEY_ID')
    const secretAccessKey = Deno.env.get('R2_SECRET_ACCESS_KEY')
    const bucket = Deno.env.get('R2_BUCKET')
    const customEndpoint = Deno.env.get('R2_ENDPOINT')
    
    const endpoint = customEndpoint || (accountId ? `https://${accountId}.r2.cloudflarestorage.com` : '')

    if (!endpoint || !accessKeyId || !secretAccessKey || !bucket) {
      throw new Error("Storage configuration error")
    }

    const r2Client = new S3Client({
      region: 'auto',
      endpoint: endpoint,
      credentials: {
        accessKeyId: accessKeyId,
        secretAccessKey: secretAccessKey,
      },
    })

    const timestamp = Date.now()
    const filePath = `modules/${module_id}/versions/v${version_code}/build_${timestamp}.apk`

    // NOTE: We don't specify ContentType here to avoid signing it. 
    // This allows the client to send it without signature mismatch issues with some OkHttp versions.
    const command = new PutObjectCommand({
      Bucket: bucket,
      Key: filePath,
    })

    const uploadUrl = await getSignedUrl(r2Client, command, { expiresIn: 900 })

    return new Response(
      JSON.stringify({
        valid: true,
        upload_url: uploadUrl,
        file_path: filePath
      }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )

  } catch (error: any) {
    return new Response(JSON.stringify({ error: error.message }), { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
  }
})
