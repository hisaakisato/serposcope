/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.controllers;

import ninja.Context;
import ninja.Result;
import ninja.Results;

public class JudgeController {

    public Result judge(Context context) {
        StringBuilder sb = new StringBuilder("Your IP: ")
            .append(context.getRemoteAddr());
        return Results
            .text()
            .render(sb.toString());
    }

}
