/*
 * Copyright 2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.componentfactory.froala.ui;

import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route("long-html")
@Menu(title = "Long HTML", order = 2)
public class LongHtmlView extends FroalaViewBase {

    public static final String HTML = """
                                                                            
                                                                             <h1>Lorem Ipsum Documentation - Comprehensive Reference Guide</h1>
            
                                                                             <h2>1. Introduction and Overview</h2>
                                                                             <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</p>
            
                                                                             <p>Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit.</p>
            
                                                                             <p>Sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur. Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur vel illum qui dolorem eum fugiat quo voluptas nulla pariatur.</p>
            
                                                                             <h2>2. Key Points and Features</h2>
                                                                             <ul>
                                                                                 <li>Consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore</li>
                                                                                 <li>Ut labore et dolore magna aliqua enim ad minim veniam quis nostrud</li>
                                                                                 <li>Quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo</li>
                                                                                 <li>Ex ea commodo consequat duis aute irure dolor in reprehenderit voluptate</li>
                                                                                 <li>Voluptate velit esse cillum dolore eu fugiat nulla pariatur excepteur</li>
                                                                                 <li>Sint occaecat cupidatat non proident sunt in culpa qui officia</li>
                                                                                 <li>Officia deserunt mollit anim id est laborum sed ut perspiciatis</li>
                                                                                 <li>Unde omnis iste natus error sit voluptatem accusantium doloremque</li>
                                                                             </ul>
            
                                                                             <p>At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias excepturi sint occaecati cupiditate non provident. Similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga. Et harum quidem rerum facilis est et expedita distinctio nam libero tempore.</p>
            
                                                                             <h2>3. Detailed Analysis - First Phase</h2>
                                                                             <p>Cum soluta nobis est eligendi optio cumque nihil impedit quo minus id quod maxime placeat facere possimus omnis voluptas assumenda est omnis dolor repellendus. Temporibus autem quibusdam et aut officiis debitis aut rerum necessitatibus saepe eveniet ut et voluptates repudiandae sint et molestiae non recusandae itaque earum rerum hic tenetur a sapiente delectus.</p>
            
                                                                             <p>Ut aut reiciendis voluptatibus maiores alias consequatur aut perferendis doloribus asperiores repellat sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium totam rem aperiam eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.</p>
            
                                                                             <table>
                                                                                 <thead>
                                                                                     <tr>
                                                                                         <th>Category</th>
                                                                                         <th>Description</th>
                                                                                         <th>Value</th>
                                                                                         <th>Status</th>
                                                                                     </tr>
                                                                                 </thead>
                                                                                 <tbody>
                                                                                     <tr>
                                                                                         <td>Lorem</td>
                                                                                         <td>Dolor sit amet consectetur adipiscing</td>
                                                                                         <td>87.5%</td>
                                                                                         <td>Active</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>Ipsum</td>
                                                                                         <td>Adipiscing elit sed do eiusmod</td>
                                                                                         <td>92.3%</td>
                                                                                         <td>Active</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>Dolor</td>
                                                                                         <td>Eiusmod tempor incididunt ut labore</td>
                                                                                         <td>76.8%</td>
                                                                                         <td>Pending</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>Sit</td>
                                                                                         <td>Labore et dolore magna aliqua</td>
                                                                                         <td>81.2%</td>
                                                                                         <td>Active</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>Amet</td>
                                                                                         <td>Aliqua ut enim ad minim</td>
                                                                                         <td>89.7%</td>
                                                                                         <td>Active</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>Consectetur</td>
                                                                                         <td>Veniam quis nostrud exercitation</td>
                                                                                         <td>78.4%</td>
                                                                                         <td>Pending</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>Adipiscing</td>
                                                                                         <td>Ullamco laboris nisi ut aliquip</td>
                                                                                         <td>85.9%</td>
                                                                                         <td>Active</td>
                                                                                     </tr>
                                                                                 </tbody>
                                                                             </table>
            
                                                                             <h2>4. Further Considerations and Implementation</h2>
                                                                             <p>Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt neque porro quisquam est qui dolorem ipsum quia dolor sit amet consectetur adipisci velit sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem.</p>
            
                                                                             <p>Ut enim ad minima veniam quis nostrum exercitationem ullam corporis suscipit laboriosam nisi ut aliquid ex ea commodi consequatur quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur vel illum qui dolorem eum fugiat quo voluptas nulla pariatur.</p>
            
                                                                             <p>At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias excepturi sint occaecati cupiditate non provident similique sunt in culpa qui officia deserunt mollitia animi id est laborum et dolorum fuga et harum quidem rerum facilis est et expedita distinctio.</p>
            
                                                                             <h2>5. Specifications and Requirements</h2>
                                                                             <ul>
                                                                                 <li>Excepteur sint occaecat cupidatat non proident sunt in culpa qui officia deserunt</li>
                                                                                 <li>Qui officia deserunt mollit anim id est laborum sed ut perspiciatis unde</li>
                                                                                 <li>Perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque</li>
                                                                                 <li>Accusantium doloremque laudantium totam rem aperiam eaque ipsa quae</li>
                                                                                 <li>Ab illo inventore veritatis et quasi architecto beatae vitae dicta</li>
                                                                                 <li>Dicta sunt explicabo nemo enim ipsam voluptatem quia voluptas sit</li>
                                                                                 <li>Sit aspernatur aut odit aut fugit sed quia consequuntur magni</li>
                                                                                 <li>Magni dolores eos qui ratione voluptatem sequi nesciunt neque</li>
                                                                                 <li>Porro quisquam est qui dolorem ipsum quia dolor sit amet</li>
                                                                                 <li>Amet consectetur adipisci velit sed quia non numquam eius</li>
                                                                             </ul>
            
                                                                             <p>Nam libero tempore cum soluta nobis est eligendi optio cumque nihil impedit quo minus id quod maxime placeat facere possimus omnis voluptas assumenda est omnis dolor repellendus temporibus autem quibusdam et aut officiis debitis aut rerum necessitatibus saepe eveniet ut et voluptates repudiandae sint et molestiae non recusandae itaque earum rerum hic tenetur a sapiente delectus.</p>
            
                                                                             <table>
                                                                                 <thead>
                                                                                     <tr>
                                                                                         <th>Metric</th>
                                                                                         <th>Q1 2024</th>
                                                                                         <th>Q2 2024</th>
                                                                                         <th>Q3 2024</th>
                                                                                         <th>Q4 2024</th>
                                                                                     </tr>
                                                                                 </thead>
                                                                                 <tbody>
                                                                                     <tr>
                                                                                         <td>Consistency</td>
                                                                                         <td>78%</td>
                                                                                         <td>82%</td>
                                                                                         <td>85%</td>
                                                                                         <td>88%</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>Reliability</td>
                                                                                         <td>89%</td>
                                                                                         <td>91%</td>
                                                                                         <td>93%</td>
                                                                                         <td>94%</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>Efficiency</td>
                                                                                         <td>72%</td>
                                                                                         <td>76%</td>
                                                                                         <td>79%</td>
                                                                                         <td>82%</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>Accuracy</td>
                                                                                         <td>94%</td>
                                                                                         <td>95%</td>
                                                                                         <td>96%</td>
                                                                                         <td>97%</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>Availability</td>
                                                                                         <td>99.2%</td>
                                                                                         <td>99.4%</td>
                                                                                         <td>99.6%</td>
                                                                                         <td>99.7%</td>
                                                                                     </tr>
                                                                                 </tbody>
                                                                             </table>
            
                                                                             <h2>6. Technical Details and In-Depth Analysis</h2>
                                                                             <p>Ut aut reiciendis voluptatibus maiores alias consequatur aut perferendis doloribus asperiores repellat sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium totam rem aperiam eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit sed quia consequuntur magni dolores.</p>
            
                                                                             <p>Eos qui ratione voluptatem sequi nesciunt neque porro quisquam est qui dolorem ipsum quia dolor sit amet consectetur adipisci velit sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam quis nostrum exercitationem ullam corporis suscipit laboriosam nisi ut aliquid ex ea commodi consequatur quis autem vel eum iure reprehenderit.</p>
            
                                                                             <p>Qui in ea voluptate velit esse quam nihil molestiae consequatur vel illum qui dolorem eum fugiat quo voluptas nulla pariatur. At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias excepturi sint occaecati cupiditate non provident similique sunt in culpa qui officia deserunt mollitia animi.</p>
            
                                                                             <h2>7. Processes and Procedures</h2>
                                                                             <ul>
                                                                                 <li>Id est laborum et dolorum fuga et harum quidem rerum facilis</li>
                                                                                 <li>Facilis est et expedita distinctio nam libero tempore cum soluta</li>
                                                                                 <li>Soluta nobis est eligendi optio cumque nihil impedit quo minus</li>
                                                                                 <li>Quo minus id quod maxime placeat facere possimus omnis voluptas</li>
                                                                                 <li>Voluptas assumenda est omnis dolor repellendus temporibus autem</li>
                                                                                 <li>Autem quibusdam et aut officiis debitis aut rerum necessitatibus</li>
                                                                                 <li>Necessitatibus saepe eveniet ut et voluptates repudiandae sint</li>
                                                                                 <li>Sint et molestiae non recusandae itaque earum rerum hic</li>
                                                                             </ul>
            
                                                                             <p>Tenetur a sapiente delectus ut aut reiciendis voluptatibus maiores alias consequatur aut perferendis doloribus asperiores repellat. Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium totam rem aperiam eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo nemo enim ipsam voluptatem quia voluptas sit aspernatur.</p>
            
                                                                             <table>
                                                                                 <thead>
                                                                                     <tr>
                                                                                         <th>Phase</th>
                                                                                         <th>Activity</th>
                                                                                         <th>Duration</th>
                                                                                         <th>Responsibility</th>
                                                                                         <th>Result</th>
                                                                                     </tr>
                                                                                 </thead>
                                                                                 <tbody>
                                                                                     <tr>
                                                                                         <td>1</td>
                                                                                         <td>Lorem ipsum dolor</td>
                                                                                         <td>2 weeks</td>
                                                                                         <td>Team A</td>
                                                                                         <td>Documentation</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>2</td>
                                                                                         <td>Consectetur adipiscing</td>
                                                                                         <td>3 weeks</td>
                                                                                         <td>Team B</td>
                                                                                         <td>Specification</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>3</td>
                                                                                         <td>Sed do eiusmod tempor</td>
                                                                                         <td>4 weeks</td>
                                                                                         <td>Team C</td>
                                                                                         <td>Implementation</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>4</td>
                                                                                         <td>Incididunt ut labore</td>
                                                                                         <td>2 weeks</td>
                                                                                         <td>Team D</td>
                                                                                         <td>Testing</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>5</td>
                                                                                         <td>Et dolore magna aliqua</td>
                                                                                         <td>1 week</td>
                                                                                         <td>Team A</td>
                                                                                         <td>Deployment</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>6</td>
                                                                                         <td>Ut enim ad minim</td>
                                                                                         <td>2 weeks</td>
                                                                                         <td>Support</td>
                                                                                         <td>Maintenance</td>
                                                                                     </tr>
                                                                                 </tbody>
                                                                             </table>
            
                                                                             <h2>8. Aut Odit Aut Fugit - Options and Choices</h2>
                                                                             <p>Sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt neque porro quisquam est qui dolorem ipsum quia dolor sit amet consectetur adipisci velit. Sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem ut enim ad minima veniam quis nostrum exercitationem ullam corporis suscipit laboriosam nisi ut aliquid ex ea commodi consequatur.</p>
            
                                                                             <p>Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur vel illum qui dolorem eum fugiat quo voluptas nulla pariatur. At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias excepturi sint occaecati cupiditate non provident similique sunt in culpa.</p>
            
                                                                             <h2>9. Advanced Metrics and Statistics</h2>
                                                                             <table>
                                                                                 <thead>
                                                                                     <tr>
                                                                                         <th>Region</th>
                                                                                         <th>Users</th>
                                                                                         <th>Engagement</th>
                                                                                         <th>Retention</th>
                                                                                         <th>ROI</th>
                                                                                     </tr>
                                                                                 </thead>
                                                                                 <tbody>
                                                                                     <tr>
                                                                                         <td>North</td>
                                                                                         <td>15,234</td>
                                                                                         <td>68%</td>
                                                                                         <td>84%</td>
                                                                                         <td>245%</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>South</td>
                                                                                         <td>12,567</td>
                                                                                         <td>72%</td>
                                                                                         <td>89%</td>
                                                                                         <td>267%</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>East</td>
                                                                                         <td>18,923</td>
                                                                                         <td>65%</td>
                                                                                         <td>81%</td>
                                                                                         <td>223%</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>West</td>
                                                                                         <td>14,789</td>
                                                                                         <td>71%</td>
                                                                                         <td>87%</td>
                                                                                         <td>256%</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>Central</td>
                                                                                         <td>19,456</td>
                                                                                         <td>74%</td>
                                                                                         <td>91%</td>
                                                                                         <td>289%</td>
                                                                                     </tr>
                                                                                 </tbody>
                                                                             </table>
            
                                                                             <h2>10. Implementation Guidelines</h2>
                                                                             <p>Qui officia deserunt mollitia animi id est laborum et dolorum fuga et harum quidem rerum facilis est et expedita distinctio nam libero tempore cum soluta nobis est eligendi optio cumque nihil impedit quo minus id quod maxime placeat facere possimus omnis voluptas assumenda est omnis dolor repellendus.</p>
            
                                                                             <p>Temporibus autem quibusdam et aut officiis debitis aut rerum necessitatibus saepe eveniet ut et voluptates repudiandae sint et molestiae non recusandae itaque earum rerum hic tenetur a sapiente delectus ut aut reiciendis voluptatibus maiores alias consequatur aut perferendis doloribus asperiores repellat sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium.</p>
            
                                                                             <ul>
                                                                                 <li>Totam rem aperiam eaque ipsa quae ab illo inventore veritatis</li>
                                                                                 <li>Quasi architecto beatae vitae dicta sunt explicabo nemo enim ipsam</li>
                                                                                 <li>Voluptatem quia voluptas sit aspernatur aut odit aut fugit sed</li>
                                                                                 <li>Quia consequuntur magni dolores eos qui ratione voluptatem sequi</li>
                                                                                 <li>Nesciunt neque porro quisquam est qui dolorem ipsum quia dolor</li>
                                                                                 <li>Sit amet consectetur adipisci velit sed quia non numquam eius</li>
                                                                                 <li>Modi tempora incidunt ut labore et dolore magnam aliquam quaerat</li>
                                                                                 <li>Voluptatem ut enim ad minima veniam quis nostrum exercitationem</li>
                                                                             </ul>
            
                                                                             <h2>11. Comparative Analysis</h2>
                                                                             <p>Ullam corporis suscipit laboriosam nisi ut aliquid ex ea commodi consequatur quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur vel illum qui dolorem eum fugiat quo voluptas nulla pariatur. At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti quos dolores.</p>
            
                                                                             <table>
                                                                                 <thead>
                                                                                     <tr>
                                                                                         <th>Approach</th>
                                                                                         <th>Advantages</th>
                                                                                         <th>Disadvantages</th>
                                                                                         <th>Costs</th>
                                                                                         <th>Recommendation</th>
                                                                                     </tr>
                                                                                 </thead>
                                                                                 <tbody>
                                                                                     <tr>
                                                                                         <td>Lorem</td>
                                                                                         <td>Fast, flexible</td>
                                                                                         <td>Complex</td>
                                                                                         <td>$10k</td>
                                                                                         <td>Yes</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>Ipsum</td>
                                                                                         <td>Simple, stable</td>
                                                                                         <td>Slow</td>
                                                                                         <td>$5k</td>
                                                                                         <td>Conditional</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>Dolor</td>
                                                                                         <td>Scalable</td>
                                                                                         <td>Expensive</td>
                                                                                         <td>$20k</td>
                                                                                         <td>For large projects</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>Sit</td>
                                                                                         <td>Reliable</td>
                                                                                         <td>Limited</td>
                                                                                         <td>$3k</td>
                                                                                         <td>For basic projects</td>
                                                                                     </tr>
                                                                                     <tr>
                                                                                         <td>Amet</td>
                                                                                         <td>Modern, trendy</td>
                                                                                         <td>Immature</td>
                                                                                         <td>$8k</td>
                                                                                         <td>To be reviewed</td>
                                                                                     </tr>
                                                                                 </tbody>
                                                                             </table>
            
                                                                             <h2>12. Conclusions and Outlook</h2>
                                                                             <p>Et quas molestias excepturi sint occaecati cupiditate non provident similique sunt in culpa qui officia deserunt mollitia animi id est laborum et dolorum fuga. Et harum quidem rerum facilis est et expedita distinctio nam libero tempore cum soluta nobis est eligendi optio cumque nihil impedit quo minus id quod maxime placeat facere possimus omnis voluptas assumenda est omnis dolor repellendus.</p>
            
                                                                             <p>Temporibus autem quibusdam et aut officiis debitis aut rerum necessitatibus saepe eveniet ut et voluptates repudiandae sint et molestiae non recusandae itaque earum rerum hic tenetur a sapiente delectus ut aut reiciendis voluptatibus maiores alias consequatur aut perferendis doloribus asperiores repellat.</p>
            
                                                                             <p>Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium totam rem aperiam eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt neque porro quisquam est qui dolorem ipsum quia dolor sit amet consectetur adipisci velit.</p>
            
                                                                             <p>Sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam quis nostrum exercitationem ullam corporis suscipit laboriosam nisi ut aliquid ex ea commodi consequatur quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur vel illum qui dolorem eum fugiat quo voluptas nulla pariatur at vero eos et accusamus.</p>
            
                                                                             <h2>13. Summary of Key Points</h2>
                                                                             <ul>
                                                                                 <li>Et iusto odio dignissimos ducimus qui blanditiis praesentium</li>
                                                                                 <li>Voluptatum deleniti atque corrupti quos dolores et quas molestias</li>
                                                                                 <li>Excepturi sint occaecati cupiditate non provident similique sunt</li>
                                                                                 <li>In culpa qui officia deserunt mollitia animi id est laborum</li>
                                                                                 <li>Et dolorum fuga et harum quidem rerum facilis est</li>
                                                                                 <li>Et expedita distinctio nam libero tempore cum soluta nobis</li>
                                                                                 <li>Est eligendi optio cumque nihil impedit quo minus id quod</li>
                                                                                 <li>Maxime placeat facere possimus omnis voluptas assumenda est</li>
                                                                                 <li>Omnis dolor repellendus temporibus autem quibusdam et aut</li>
                                                                                 <li>Officiis debitis aut rerum necessitatibus saepe eveniet ut</li>
                                                                             </ul>
            
                                                                             <h2>14. Outlook and Next Steps</h2>
                                                                             <p>Et voluptates repudiandae sint et molestiae non recusandae itaque earum rerum hic tenetur a sapiente delectus ut aut reiciendis voluptatibus maiores alias consequatur aut perferendis doloribus asperiores repellat. Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium totam rem aperiam eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo nemo enim ipsam voluptatem quia voluptas sit aspernatur.</p>
            
                                                                             <p>Aut odit aut fugit sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt neque porro quisquam est qui dolorem ipsum quia dolor sit amet consectetur adipisci velit sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem ut enim ad minima veniam quis nostrum exercitationem ullam corporis suscipit laboriosam nisi ut aliquid ex ea commodi consequatur.</p>
            
                                                                             <h2>15. Final Considerations</h2>
                                                                             <p>Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur vel illum qui dolorem eum fugiat quo voluptas nulla pariatur. At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias excepturi sint occaecati cupiditate non provident similique sunt in culpa qui officia deserunt mollitia animi id est laborum et dolorum fuga.</p>
            
                                                                             <p>Et harum quidem rerum facilis est et expedita distinctio nam libero tempore cum soluta nobis est eligendi optio cumque nihil impedit quo minus id quod maxime placeat facere possimus omnis voluptas assumenda est omnis dolor repellendus temporibus autem quibusdam et aut officiis debitis aut rerum necessitatibus saepe eveniet ut et voluptates repudiandae sint et molestiae non recusandae itaque earum rerum hic tenetur a sapiente delectus.</p>
            
                                                                             <p>Ut aut reiciendis voluptatibus maiores alias consequatur aut perferendis doloribus asperiores repellat sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium totam rem aperiam eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt.</p>
                                                                          """;

    public LongHtmlView() {
        getEditor().setValue(HTML);
        getEditor().setSizeFull();

    }

}
