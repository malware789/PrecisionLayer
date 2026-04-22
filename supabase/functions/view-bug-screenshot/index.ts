import { createClient } from "npm:@supabase/supabase-js@2"
import { S3Client, GetObjectCommand } from "npm:@aws-sdk/client-s3@3.341.0"
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
    const { bug_id } = payload

    if (!bug_id) {
      return new Response(JSON.stringify({ error: 'Missing bug_id' }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 2. Fetch bug from database
    const { data: bug, error: bugError } = await supabase
      .from('bug_reports')
      .select('image_path, workspace_id')
      .eq('id', bug_id)
      .maybeSingle()

    if (bugError) {
      return new Response(JSON.stringify({ error: 'Database error' }), { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    if (!bug) {
      return new Response(JSON.stringify({ error: 'Bug not found' }), { status: 404, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    if (!bug.image_path) {
      return new Response(JSON.stringify({ view_url: null, message: 'No screenshot attached to this bug' }), { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 3. Verify workspace membership
    const { data: membership, error: memberError } = await supabase
      .from('workspace_members')
      .select('user_id')
      .eq('workspace_id', bug.workspace_id)
      .eq('user_id', user.id)
      .maybeSingle()

    if (memberError || !membership) {
      return new Response(JSON.stringify({ error: 'Forbidden: You do not have access to this workspace' }), { status: 403, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 4. Validate image path
    if (!bug.image_path.startsWith(`bugs/${bug.workspace_id}/`)) {
      return new Response(JSON.stringify({ error: 'Invalid image path structure' }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 5. Setup R2 Client
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

    // 6. Generate signed read URL
    const command = new GetObjectCommand({
      Bucket: bucket,
      Key: bug.image_path
    })

    const viewUrl = await getSignedUrl(r2Client, command, { expiresIn: 3600 }) // 1 hour expiry
    
    return new Response(
      JSON.stringify({ view_url: viewUrl }),
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
