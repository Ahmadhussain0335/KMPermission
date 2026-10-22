package io.github.ahmadhussain0335.kmppermission

/**
 * The outcome of a [PermissionManager.request] call.
 *
 * Structurally identical to [PermissionStatus]: requesting a permission always resolves to one of
 * the same states you would see from [PermissionManager.check]. The distinct name exists purely
 * for call-site clarity — a [PermissionResult] is something you just asked for, a [PermissionStatus]
 * is something you are inspecting at rest — without duplicating the state machine.
 */
public typealias PermissionResult = PermissionStatus
