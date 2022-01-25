package net.wiringbits.components.widgets

import com.alexitc.materialui.facade.materialUiCore.{components => mui, materialUiCoreStrings => muiStrings}
import net.wiringbits.common.models.UserToken
import net.wiringbits.forms.ResetPasswordFormData
import net.wiringbits.models.User
import net.wiringbits.ui.components.inputs.PasswordInput
import net.wiringbits.webapp.utils.slinkyUtils.components.core.ErrorLabel
import net.wiringbits.webapp.utils.slinkyUtils.components.core.widgets.{CircularLoader, Container}
import net.wiringbits.webapp.utils.slinkyUtils.forms.StatefulFormData
import net.wiringbits.{API, AppStrings}
import org.scalajs.dom
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global
import slinky.core.annotations.react
import slinky.core.facade.{Fragment, Hooks}
import slinky.core.{FunctionalComponent, SyntheticEvent}
import slinky.web.html.{form, onSubmit}
import typings.reactRouter.mod.useHistory

import scala.util.{Failure, Success}

@react object ResetPasswordForm {
  case class Props(api: API, loggedIn: User => Unit, token: Option[UserToken])

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] { props =>
    val history = useHistory()

    val (formData, setFormData) = Hooks.useState(
      StatefulFormData(
        ResetPasswordFormData.initial(
          passwordLabel = AppStrings.password,
          repeatPasswordLabel = AppStrings.repeatPassword,
          token = props.token
        )
      )
    )

    def onDataChanged(f: ResetPasswordFormData => ResetPasswordFormData): Unit = {
      setFormData { current =>
        current.filling.copy(data = f(current.data))
      }
    }

    def handleSubmit(e: SyntheticEvent[_, dom.Event]): Unit = {
      e.preventDefault()

      if (formData.isSubmitButtonEnabled) {
        setFormData(_.submit)
        for {
          request <- formData.data.submitRequest
            .orElse {
              setFormData(_.submissionFailed("Complete the necessary data"))
              None
            }
        } yield props.api.client
          .resetPassword(request)
          .onComplete {
            case Success(res) =>
              props.loggedIn(User(name = res.name, email = res.email, jwt = res.token))
              setFormData(_.submitted)
              history.push("/dashboard")
            case Failure(ex) =>
              setFormData(_.submissionFailed(ex.getMessage))
          }
      } else {
        println("Submit fired when it is not available")
      }
    }

    val passwordInput = PasswordInput
      .component(
        PasswordInput.Props(
          formData.data.password,
          disabled = formData.isInputDisabled,
          onChange = value => onDataChanged(x => x.copy(password = x.password.updated(value)))
        )
      )

    val repeatPasswordInput = PasswordInput
      .component(
        PasswordInput.Props(
          formData.data.repeatPassword,
          disabled = formData.isInputDisabled,
          onChange = value => onDataChanged(x => x.copy(repeatPassword = x.repeatPassword.updated(value)))
        )
      )

    val resetPasswordButton = {
      val text = if (formData.isSubmitting) {
        Fragment(
          CircularLoader(),
          Container(margin = Container.EdgeInsets.left(8), child = AppStrings.loading)
        )
      } else {
        Fragment(AppStrings.resetPassword)
      }

      mui
        .Button(text)
        .fullWidth(true)
        .disabled(formData.isSubmitButtonDisabled)
        .variant(muiStrings.contained)
        .color(muiStrings.primary)
        .size(muiStrings.large)
        .`type`(muiStrings.submit)
    }

    form(onSubmit := (handleSubmit(_)))(
      Container(
        margin = Container.EdgeInsets.all(16),
        alignItems = Container.Alignment.center,
        child = Fragment(
          passwordInput,
          repeatPasswordInput,
          formData.firstValidationError.map { text =>
            Container(
              margin = Container.EdgeInsets.top(16),
              child = ErrorLabel(text)
            )
          }
        )
      ),
      Container(
        alignItems = Container.Alignment.center,
        child = Fragment(
          resetPasswordButton
        )
      )
    )
  }
}