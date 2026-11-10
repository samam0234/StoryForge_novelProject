import { Component } from "@angular/core";
import { bootstrapApplication } from "@angular/platform-browser";
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { provideRouter, RouterOutlet } from "@angular/router";
import { routes } from "./app/app.routes";
import { authInterceptor } from "./app/core/auth";

@Component({
  selector: "sf-root",
  imports: [RouterOutlet],
  template: "<router-outlet />",
})
class App {}

bootstrapApplication(App, {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
}).catch((error: unknown) => console.error(error));
