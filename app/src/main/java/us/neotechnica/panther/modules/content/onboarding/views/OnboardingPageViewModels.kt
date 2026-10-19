//
//  OnboardingPageViewModels.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views

import us.neotechnica.panther.bundle.navigators.OnboardingNavigatorState
import us.neotechnica.panther.modules.content.onboarding.views.authcodepageview.AuthCodePageReducer
import us.neotechnica.panther.modules.content.onboarding.views.permissionpageview.PermissionPageReducer
import us.neotechnica.panther.modules.content.onboarding.views.selectlanguagepageview.SelectLanguagePageReducer
import us.neotechnica.panther.modules.content.onboarding.views.signinpageview.SignInPageReducer
import us.neotechnica.panther.modules.content.onboarding.views.verifynumberpageview.VerifyNumberPageReducer
import us.neotechnica.panther.modules.content.onboarding.views.welcomepageview.WelcomePageReducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

/**
 * The onboarding pages' view models, held for the flow's lifetime
 * so a page keeps its state beneath a push and across a pop and
 * re-push.
 *
 * Each view model is created on first access and closed when the
 * flow ends; its first appearance is recorded so the page's
 * first-appearance work runs once per lifetime.
 */
internal class OnboardingPageViewModels {
    // MARK: - Properties

    private val appearedPaths = mutableSetOf<OnboardingNavigatorState.SeguePaths?>()
    private val pushedViewModels = mutableMapOf<OnboardingNavigatorState.SeguePaths, ViewModel<*, *>>()

    /** The welcome page's view model. */
    val welcome: ViewModel<WelcomePageReducer.State, WelcomePageReducer.Action> by lazy {
        ViewModel(WelcomePageReducer.State(), WelcomePageReducer())
    }

    // MARK: - Computed Properties

    /** The auth code page's view model, created on first access. */
    val authCode: ViewModel<AuthCodePageReducer.State, AuthCodePageReducer.Action>
        get() = viewModel(OnboardingNavigatorState.SeguePaths.AuthCode) { ViewModel(AuthCodePageReducer.State(), AuthCodePageReducer()) }

    /** The permission page's view model, created on first access. */
    val permission: ViewModel<PermissionPageReducer.State, PermissionPageReducer.Action>
        get() = viewModel(OnboardingNavigatorState.SeguePaths.Permission) { ViewModel(PermissionPageReducer.State(), PermissionPageReducer()) }

    /**
     * The select language page's view model, created on first
     * access.
     */
    val selectLanguage: ViewModel<SelectLanguagePageReducer.State, SelectLanguagePageReducer.Action>
        get() =
            viewModel(OnboardingNavigatorState.SeguePaths.SelectLanguage) {
                ViewModel(SelectLanguagePageReducer.State(), SelectLanguagePageReducer())
            }

    /** The sign in page's view model, created on first access. */
    val signIn: ViewModel<SignInPageReducer.State, SignInPageReducer.Action>
        get() = viewModel(OnboardingNavigatorState.SeguePaths.SignIn) { ViewModel(SignInPageReducer.State(), SignInPageReducer()) }

    /**
     * The verify number page's view model, created on first
     * access.
     */
    val verifyNumber: ViewModel<VerifyNumberPageReducer.State, VerifyNumberPageReducer.Action>
        get() =
            viewModel(OnboardingNavigatorState.SeguePaths.VerifyNumber) {
                ViewModel(VerifyNumberPageReducer.State(), VerifyNumberPageReducer())
            }

    // MARK: - Methods

    /** Closes every view model; the flow has ended. */
    fun closeAll() {
        pushedViewModels.values.forEach { it.close() }
        pushedViewModels.clear()
        welcome.close()
        appearedPaths.clear()
    }

    /**
     * Runs the given first-appearance work once per view-model
     * lifetime for the page at the given path (`null` for the
     * welcome page).
     *
     * @param path The page's path, or `null` for the welcome
     *   page.
     * @param perform The first-appearance work.
     */
    fun onFirstAppear(
        path: OnboardingNavigatorState.SeguePaths?,
        perform: () -> Unit,
    ) {
        if (!appearedPaths.add(path)) return
        perform()
    }

    // MARK: - Auxiliary

    @Suppress("UNCHECKED_CAST")
    private fun <State, Action> viewModel(
        path: OnboardingNavigatorState.SeguePaths,
        create: () -> ViewModel<State, Action>,
    ): ViewModel<State, Action> = pushedViewModels.getOrPut(path, create) as ViewModel<State, Action>
}
