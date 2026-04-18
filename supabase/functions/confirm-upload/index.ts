import { createClient } from "npm:@supabase/supabase-js@2"

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
    
    if (!supabaseUrl || !supabaseKey) throw new Error("Missing config")

    const supabase = createClient(supabaseUrl, supabaseKey)

    // 1. Identify User
    const authHeader = req.headers.get('Authorization')
    if (!authHeader) throw new Error('Unauthorized')
    
    const token = authHeader.replace('Bearer ', '')
    const { data: { user }, error: userError } = await supabase.auth.getUser(token)
    if (userError || !user) throw new Error('Unauthorized')

    const payload = await req.json()
    const { 
      module_id, 
      version_name, 
      version_code, 
      package_name, 
      file_path, 
      file_size,
      file_type,
      version_title,
      release_notes 
    } = payload

    if (!module_id || !version_name || !version_code || !file_path) {
      throw new Error('Missing fields')
    }

    // 2. Fetch module & Workspace Security
    const { data: module, error: moduleError } = await supabase
      .from('modules')
      .select('package_name, workspace_id')
      .eq('id', module_id)
      .single()

    if (moduleError || !module) throw new Error('Module not found')

    // Verify workspace membership
    const { data: membership, error: memberError } = await supabase
      .from('workspace_members')
      .select('user_id')
      .eq('workspace_id', module.workspace_id)
      .eq('user_id', user.id)
      .maybeSingle()

    if (memberError || !membership) throw new Error('Access denied to workspace')

    if (package_name && module.package_name !== package_name) {
      throw new Error('Package mismatch')
    }

    // 3. Compute build_number (Atomic server-side calculation)
    const { data: lastBuild, error: buildError } = await supabase
      .from('app_versions')
      .select('build_number')
      .eq('module_id', module_id)
      .eq('version_name', version_name)
      .order('build_number', { ascending: false })
      .limit(1)
      .maybeSingle()

    const buildNumber = (lastBuild?.build_number ?? 0) + 1

    // 4. Insert app_version
    const { data: newVersion, error: insertError } = await supabase
      .from('app_versions')
      .insert({
        module_id,
        workspace_id: module.workspace_id,
        version_name,
        version_code,
        build_number: buildNumber,
        version_title: version_title || null,
        release_notes: release_notes || null,
        file_url: file_path, 
        file_size: file_size || 0,
        file_type: file_type || 'apk',
        created_by: user.id
      })
      .select()
      .single()

    if (insertError) throw insertError

    return new Response(
      JSON.stringify(newVersion),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )

  } catch (error: any) {
    return new Response(JSON.stringify({ error: error.message }), { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
  }
})
