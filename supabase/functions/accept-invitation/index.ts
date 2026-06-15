import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const { invitation_id } = await req.json()

    if (!invitation_id) {
      return new Response(JSON.stringify({ error: 'invitation_id is required' }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 400,
      })
    }

    const authHeader = req.headers.get('Authorization')
    if (!authHeader) {
      return new Response(JSON.stringify({ error: 'Missing Authorization header' }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 401,
      })
    }

    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      { global: { headers: { Authorization: authHeader } } }
    )

    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    // Get the user from the auth token
    const { data: { user }, error: userError } = await supabaseClient.auth.getUser()
    
    if (userError || !user) {
      return new Response(JSON.stringify({ error: 'Unauthorized' }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 401,
      })
    }

    // 1. Fetch the invitation
    const { data: invitation, error: inviteError } = await supabaseAdmin
      .from('workspace_invitations')
      .select('*')
      .eq('id', invitation_id)
      .single()

    if (inviteError || !invitation) {
      return new Response(JSON.stringify({ error: 'Invitation not found' }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 404,
      })
    }

    // 2. Verification
    if (invitation.status !== 'pending') {
      return new Response(JSON.stringify({ error: `Invitation is ${invitation.status}` }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 400,
      })
    }

    if (invitation.email !== user.email) {
      return new Response(JSON.stringify({ error: 'Email mismatch' }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 403,
      })
    }

    if (new Date(invitation.expires_at) < new Date()) {
       // Mark as expired
       await supabaseAdmin.from('workspace_invitations').update({ status: 'expired' }).eq('id', invitation_id)
       return new Response(JSON.stringify({ error: 'Invitation expired' }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 400,
      })
    }

    // 3. Verify user is not already a member
    const { data: existingMember, error: existingMemberError } = await supabaseAdmin
      .from('workspace_members')
      .select('role')
      .eq('workspace_id', invitation.workspace_id)
      .eq('user_id', user.id)
      .maybeSingle()

    if (existingMember) {
      // Option A approach: Gracefully handle duplicate by returning a specific error
      return new Response(JSON.stringify({ error: 'already_member' }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 409,
      })
    }

    // 4. Execution
    // Insert into workspace_members
    const { error: memberError } = await supabaseAdmin
      .from('workspace_members')
      .insert({
        workspace_id: invitation.workspace_id,
        user_id: user.id,
        role: invitation.role
      })

    if (memberError) {
      throw memberError
    }

    // Update invitation
    const { error: updateError } = await supabaseAdmin
      .from('workspace_invitations')
      .update({
        status: 'accepted',
        accepted_by: user.id,
        accepted_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      })
      .eq('id', invitation_id)

    if (updateError) {
      throw updateError
    }

    return new Response(JSON.stringify({ success: true, workspace_id: invitation.workspace_id }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 200,
    })
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 500,
    })
  }
})
