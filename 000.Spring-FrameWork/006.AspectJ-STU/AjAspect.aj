public aspect AjAspect {

    pointcut say():
            execution(* App.say(..));
    before(): say() {
        System.out.println("AjAspect before say");
    }
    after(): say() {
        System.out.println("AjAspect after say");
    }
}